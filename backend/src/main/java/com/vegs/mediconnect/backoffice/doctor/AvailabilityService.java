package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorDayOff;
import com.vegs.mediconnect.datasource.doctor.DoctorDayOffRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorHours;
import com.vegs.mediconnect.datasource.doctor.DoctorHoursRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A doctor's availability: the usual week, the days off, and the slots in
 * the diary that follow from them (board B06).
 *
 * Saving the week rewrites the diary from tomorrow to the end of the
 * booking horizon. Booked appointments are never moved: a slot someone
 * holds stays exactly where it is, even if it now falls outside the
 * doctor's hours, and is counted so the desk can see it. Only slots
 * nobody ever booked are removed; one with a cancelled booking behind it
 * is blocked instead, so the cancelled visit still points at something.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final DoctorRepository doctorRepository;
    private final DoctorHoursRepository hoursRepository;
    private final DoctorDayOffRepository dayOffRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;
    private final WaitlistEntryRepository waitlistRepository;

    /** What a save did to the diary, for the message afterwards. */
    public record Regenerated(int created, int removed, int keptBooked) {
    }

    /** A day off, and how many patients it leaves to rebook. */
    public record DayOff(UUID id, LocalDate date, String reason, int toRebook) {
    }

    /** The saved week, or one read off the diary when none was ever saved. */
    public record Editor(WeeklyHours hours, boolean inferred, List<DayOff> daysOff) {
    }

    @Transactional(readOnly = true)
    public Editor editor(UUID doctorId) {
        Doctor doctor = doctor(doctorId);
        List<DoctorHours> saved = hoursRepository.findAllByDoctorOrderByDayOfWeekAscStartTimeAsc(doctor);
        WeeklyHours hours;
        boolean inferred = saved.isEmpty();
        if (inferred) {
            hours = inferFromDiary(doctor);
        } else {
            Map<DayOfWeek, List<WeeklyHours.Range>> days = new EnumMap<>(DayOfWeek.class);
            for (DoctorHours row : saved) {
                days.computeIfAbsent(row.getDayOfWeek(), key -> new ArrayList<>())
                        .add(new WeeklyHours.Range(row.getStartTime(), row.getEndTime()));
            }
            hours = new WeeklyHours(days, doctor.slotLength(), doctor.horizonWeeks());
        }
        return new Editor(hours, inferred, daysOff(doctor));
    }

    private WeeklyHours inferFromDiary(Doctor doctor) {
        LocalDate from = LocalDate.now().plusDays(1);
        Map<DayOfWeek, List<LocalTime>> seen = new EnumMap<>(DayOfWeek.class);
        for (ScheduleTime slot : scheduleTimeRepository.findAllBetween(from, from.plusDays(13))) {
            if (slot.getSchedule().getDoctor().getId().equals(doctor.getId())) {
                seen.computeIfAbsent(slot.getSchedule().getDate().getDayOfWeek(), key -> new ArrayList<>())
                        .add(slot.getTime());
            }
        }
        return WeeklyHours.infer(seen, doctor.slotLength(), doctor.horizonWeeks());
    }

    private List<DayOff> daysOff(Doctor doctor) {
        LocalDate today = LocalDate.now();
        List<DayOff> list = new ArrayList<>();
        for (DoctorDayOff day : dayOffRepository.findAllByDoctorOrderByDateAsc(doctor)) {
            if (day.getDate().isBefore(today)) {
                continue;
            }
            int toRebook = (int) appointmentRepository.findAllOnDay(day.getDate()).stream()
                    .filter(appointment -> appointment.getDoctor().getId().equals(doctor.getId()))
                    .filter(AvailabilityService::active)
                    .count();
            list.add(new DayOff(day.getId(), day.getDate(), day.getReason(), toRebook));
        }
        return list;
    }

    /**
     * Saves the week and rewrites the diary to match it.
     *
     * @throws IllegalArgumentException with the problems, when the week
     *                                  cannot be saved as given
     */
    @Transactional
    public Regenerated save(UUID doctorId, WeeklyHours hours) {
        List<String> problems = hours.problems();
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", problems));
        }
        Doctor doctor = doctor(doctorId);
        doctor.setSlotMinutes(hours.slotMinutes());
        doctor.setBookingWeeks(hours.weeks());
        doctorRepository.save(doctor);

        hoursRepository.deleteAllByDoctor(doctor);
        hoursRepository.flush();
        List<DoctorHours> rows = new ArrayList<>();
        hours.days().forEach((day, ranges) -> {
            for (WeeklyHours.Range range : ranges) {
                var row = new DoctorHours();
                row.setDoctor(doctor);
                row.setDayOfWeek(day);
                row.setStartTime(range.start());
                row.setEndTime(range.end());
                rows.add(row);
            }
        });
        hoursRepository.saveAll(rows);

        LocalDate from = LocalDate.now().plusDays(1);
        return regenerate(doctor, hours, from, LocalDate.now().plusWeeks(hours.weeks()));
    }

    /** Brings the diary between two dates into line with the week. */
    Regenerated regenerate(Doctor doctor, WeeklyHours hours, LocalDate from, LocalDate to) {
        Map<LocalDate, Map<LocalTime, ScheduleTime>> diary = new HashMap<>();
        for (ScheduleTime slot : scheduleTimeRepository.findAllBetween(from, to)) {
            if (slot.getSchedule().getDoctor().getId().equals(doctor.getId())) {
                diary.computeIfAbsent(slot.getSchedule().getDate(), key -> new HashMap<>())
                        .put(slot.getTime(), slot);
            }
        }
        Map<UUID, List<Appointment>> bookings = new HashMap<>();
        for (Appointment appointment : appointmentRepository.findAllBetween(from, to)) {
            bookings.computeIfAbsent(appointment.getScheduleTime().getId(), key -> new ArrayList<>())
                    .add(appointment);
        }
        Set<UUID> held = waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED)
                .stream()
                .map(WaitlistEntry::getOfferedSlot)
                .filter(java.util.Objects::nonNull)
                .map(ScheduleTime::getId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<LocalDate> off = dayOffRepository.findAllByDoctorAndDateBetween(doctor, from, to).stream()
                .map(DoctorDayOff::getDate)
                .collect(Collectors.toSet());

        int created = 0;
        int removed = 0;
        int kept = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<LocalTime> wanted = hours.timesOn(date.getDayOfWeek());
            Map<LocalTime, ScheduleTime> existing = diary.getOrDefault(date, Map.of());
            boolean dayOff = off.contains(date);

            Schedule schedule = null;
            for (LocalTime time : wanted) {
                if (existing.containsKey(time)) {
                    continue;
                }
                if (schedule == null) {
                    schedule = scheduleFor(doctor, date);
                }
                var slot = new ScheduleTime();
                slot.setSchedule(schedule);
                slot.setTime(time);
                slot.setAvailable(!dayOff);
                slot.setBlocked(dayOff);
                scheduleTimeRepository.save(slot);
                created++;
            }

            for (ScheduleTime slot : existing.values()) {
                if (wanted.contains(slot.getTime())) {
                    continue;
                }
                List<Appointment> onSlot = bookings.getOrDefault(slot.getId(), List.of());
                if (onSlot.stream().anyMatch(AvailabilityService::active) || held.contains(slot.getId())) {
                    kept++;
                } else if (!onSlot.isEmpty()) {
                    slot.setAvailable(false);
                    slot.setBlocked(true);
                    scheduleTimeRepository.save(slot);
                    removed++;
                } else {
                    scheduleTimeRepository.delete(slot);
                    removed++;
                }
            }
        }
        return new Regenerated(created, removed, kept);
    }

    private Schedule scheduleFor(Doctor doctor, LocalDate date) {
        return scheduleRepository.findByDoctorIdAndDate(doctor.getId(), date).orElseGet(() -> {
            var schedule = new Schedule();
            schedule.setDoctor(doctor);
            schedule.setDate(date);
            schedule.setAvailable(true);
            return scheduleRepository.save(schedule);
        });
    }

    /**
     * Marks a day off and blocks its free slots. Returns how many patients
     * are already booked that day and now need a new time.
     */
    @Transactional
    public int addDayOff(UUID doctorId, LocalDate date, String reason) {
        Doctor doctor = doctor(doctorId);
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A day off has to be today or later.");
        }
        if (!dayOffRepository.existsByDoctorAndDate(doctor, date)) {
            var day = new DoctorDayOff();
            day.setDoctor(doctor);
            day.setDate(date);
            day.setReason(reason == null || reason.isBlank() ? null : reason.trim());
            dayOffRepository.save(day);
        }

        Set<UUID> held = heldSlots();
        for (ScheduleTime slot : scheduleTimeRepository.findAllBetween(date, date)) {
            if (slot.getSchedule().getDoctor().getId().equals(doctorId)
                    && Boolean.TRUE.equals(slot.getAvailable())
                    && !held.contains(slot.getId())) {
                slot.setAvailable(false);
                slot.setBlocked(true);
                scheduleTimeRepository.save(slot);
            }
        }
        return (int) appointmentRepository.findAllOnDay(date).stream()
                .filter(appointment -> appointment.getDoctor().getId().equals(doctorId))
                .filter(AvailabilityService::active)
                .count();
    }

    /** Takes a day off back: its blocked slots are open again. */
    @Transactional
    public void removeDayOff(UUID dayOffId) {
        DoctorDayOff day = dayOffRepository.findById(dayOffId).orElseThrow(NotFoundException::new);
        // Only the doctor's usual times reopen; a slot blocked because it
        // fell outside their hours stays blocked.
        List<LocalTime> usual = editor(day.getDoctor().getId()).hours().timesOn(day.getDate().getDayOfWeek());
        for (ScheduleTime slot : scheduleTimeRepository.findAllBetween(day.getDate(), day.getDate())) {
            if (slot.getSchedule().getDoctor().getId().equals(day.getDoctor().getId())
                    && Boolean.TRUE.equals(slot.getBlocked())
                    && usual.contains(slot.getTime())) {
                slot.setBlocked(false);
                slot.setAvailable(true);
                scheduleTimeRepository.save(slot);
            }
        }
        dayOffRepository.delete(day);
    }

    private Set<UUID> heldSlots() {
        return waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED).stream()
                .map(WaitlistEntry::getOfferedSlot)
                .filter(java.util.Objects::nonNull)
                .map(ScheduleTime::getId)
                .collect(Collectors.toSet());
    }

    private Doctor doctor(UUID id) {
        return doctorRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    static boolean active(Appointment appointment) {
        return !Boolean.TRUE.equals(appointment.getCanceled())
                && !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
    }
}
