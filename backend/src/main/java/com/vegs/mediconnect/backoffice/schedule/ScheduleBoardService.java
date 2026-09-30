package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.backoffice.appointment.AppointmentsView;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Reads the schedule for the board and carries out what the desk does to
 * a single slot: offer it to the waitlist, block it, or release a hold.
 */
@Service
@RequiredArgsConstructor
public class ScheduleBoardService {

    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;
    private final WaitlistEntryRepository waitlistRepository;
    private final DoctorRepository doctorRepository;
    private final WaitlistService waitlistService;

    // ---- the boards -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ScheduleBoard.Day day(LocalDate date, String specialty) {
        return ScheduleBoard.build(date,
                scheduleTimeRepository.findAllBetween(date, date),
                appointmentRepository.findAllOnDay(date),
                holds(),
                specialty,
                LocalDateTime.now());
    }

    /** One line of the week view: a doctor, and how full each day is. */
    public record WeekRow(String doctor, String specialty, String initials, String avatarClass,
                          List<WeekCell> days) {
    }

    public record WeekCell(LocalDate date, int booked, int total) {

        public boolean isWorking() {
            return total > 0;
        }

        public int getPercent() {
            return total == 0 ? 0 : Math.round(booked * 100f / total);
        }
    }

    public record Week(LocalDate monday, List<LocalDate> days, List<WeekRow> rows) {
    }

    @Transactional(readOnly = true)
    public Week week(LocalDate date, String specialty) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(monday.plusDays(i));
        }

        var slots = scheduleTimeRepository.findAllBetween(monday, sunday);
        Map<UUID, Integer> activeBySlot = new HashMap<>();
        for (Appointment appointment : appointmentRepository.findAllBetween(monday, sunday)) {
            if (ScheduleBoard.active(appointment)) {
                activeBySlot.put(appointment.getScheduleTime().getId(), 1);
            }
        }

        Map<UUID, Map<LocalDate, int[]>> counts = new LinkedHashMap<>();
        Map<UUID, Doctor> doctors = new HashMap<>();
        slots.stream()
                .filter(slot -> specialty == null || specialty.isBlank()
                        || specialty.equalsIgnoreCase(slot.getSchedule().getDoctor().getSpecialty()))
                .sorted(Comparator.comparing(slot -> slot.getSchedule().getDoctor().getLastName()))
                .forEach(slot -> {
                    Doctor doctor = slot.getSchedule().getDoctor();
                    doctors.put(doctor.getId(), doctor);
                    int[] count = counts.computeIfAbsent(doctor.getId(), key -> new HashMap<>())
                            .computeIfAbsent(slot.getSchedule().getDate(), key -> new int[2]);
                    if (!Boolean.TRUE.equals(slot.getBlocked())) {
                        count[1]++;
                    }
                    if (activeBySlot.containsKey(slot.getId())) {
                        count[0]++;
                    }
                });

        List<WeekRow> rows = new ArrayList<>();
        counts.forEach((doctorId, byDay) -> {
            Doctor doctor = doctors.get(doctorId);
            String name = Display.name(doctor.getFirstName(), doctor.getLastName());
            List<WeekCell> cells = days.stream()
                    .map(day -> {
                        int[] count = byDay.getOrDefault(day, new int[2]);
                        return new WeekCell(day, count[0], count[1]);
                    })
                    .toList();
            rows.add(new WeekRow("Dr. " + doctor.getLastName(), doctor.getSpecialty(),
                    Display.initials(name), Display.avatarClass(name), cells));
        });
        return new Week(monday, days, rows);
    }

    @Transactional(readOnly = true)
    public List<String> specialties() {
        return doctorRepository.findAll().stream()
                .map(Doctor::getSpecialty)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /** Every slot currently held for someone on the waitlist. */
    private Map<UUID, ScheduleBoard.Hold> holds() {
        Map<UUID, ScheduleBoard.Hold> holds = new HashMap<>();
        for (WaitlistEntry entry : waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED)) {
            if (entry.getOfferedSlot() != null && entry.getOfferExpiresAt() != null) {
                holds.put(entry.getOfferedSlot().getId(), new ScheduleBoard.Hold(
                        Display.name(entry.getPatient().getFirstName(), entry.getPatient().getLastName()),
                        Display.local(entry.getOfferExpiresAt())));
            }
        }
        return holds;
    }

    // ---- one slot ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SlotPanel panel(UUID slotId, boolean deskMayAct) {
        ScheduleTime slot = scheduleTimeRepository.findById(slotId).orElseThrow(NotFoundException::new);
        LocalDate date = slot.getSchedule().getDate();
        LocalDateTime now = LocalDateTime.now();
        Doctor doctor = slot.getSchedule().getDoctor();

        List<Appointment> onSlot = appointmentRepository.findAllOnDay(date).stream()
                .filter(appointment -> appointment.getScheduleTime().getId().equals(slotId))
                .toList();
        Appointment booked = onSlot.stream().filter(ScheduleBoard::active).findFirst().orElse(null);
        boolean wasCancelled = onSlot.stream().anyMatch(appointment -> Boolean.TRUE.equals(appointment.getCanceled()));
        WaitlistEntry holding = waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED)
                .stream()
                .filter(entry -> entry.getOfferedSlot() != null && entry.getOfferedSlot().getId().equals(slotId))
                .findFirst()
                .orElse(null);
        ScheduleBoard.Hold hold = holding == null ? null : new ScheduleBoard.Hold(
                Display.name(holding.getPatient().getFirstName(), holding.getPatient().getLastName()),
                Display.local(holding.getOfferExpiresAt()));

        ScheduleBoard.Cell cell = ScheduleBoard.cellFor(slot, booked, wasCancelled, hold, now);
        boolean ahead = slot.getDateTime().isAfter(now);
        boolean free = booked == null && holding == null && Boolean.TRUE.equals(slot.getAvailable())
                && !Boolean.TRUE.equals(slot.getBlocked());

        long whoFit = waitlistRepository.findAllByDoctorAndStatusOrderByDateCreatedAsc(doctor, WaitlistStatus.WAITING)
                .stream()
                .filter(entry -> entry.wants(date))
                .filter(entry -> !slotId.equals(entry.getPassedSlotId()))
                .count();

        String appointmentLink = booked == null ? null
                : "/appointments?tab=" + AppointmentsView.tabOf(booked, now.toLocalDate()).getKey()
                + "&selected=" + booked.getId();
        String holdLine = holding == null ? null
                : Display.name(holding.getPatient().getFirstName(), holding.getPatient().getLastName())
                + " has until " + Display.time(hold.until().toLocalTime())
                + " to take it, in place of their visit on " + Display.day(holding.getCurrentAppointmentDate()) + ".";

        return new SlotPanel(
                slot.getId(),
                "Dr. " + doctor.getLastName() + " · " + Display.shortTime(slot.getTime()),
                Display.longDay(date),
                cell,
                appointmentLink,
                holdLine,
                (int) whoFit,
                deskMayAct && free && ahead,
                deskMayAct && free && ahead && whoFit > 0,
                deskMayAct && free && ahead,
                deskMayAct && Boolean.TRUE.equals(slot.getBlocked()) && booked == null,
                deskMayAct && holding != null);
    }

    /**
     * Holds a free slot for whoever has waited longest and can use it.
     * Returns their name, or null when nobody on the list fits.
     */
    @Transactional
    public String offerToWaitlist(UUID slotId) {
        ScheduleTime slot = scheduleTimeRepository.findById(slotId).orElseThrow(NotFoundException::new);
        if (!Boolean.TRUE.equals(slot.getAvailable()) || Boolean.TRUE.equals(slot.getBlocked())) {
            return null;
        }
        if (!waitlistService.offerSlot(slot, null)) {
            return null;
        }
        return waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED).stream()
                .filter(entry -> entry.getOfferedSlot() != null && entry.getOfferedSlot().getId().equals(slotId))
                .map(entry -> entry.getPatient().getFirstName())
                .findFirst()
                .orElse("the next person");
    }

    /**
     * Takes a free slot out of the diary (a meeting, a room problem). Only
     * a free one: blocking a booked slot would leave a patient holding an
     * appointment the schedule says does not exist.
     */
    @Transactional
    public boolean block(UUID slotId) {
        ScheduleTime slot = scheduleTimeRepository.findById(slotId).orElseThrow(NotFoundException::new);
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            return false;
        }
        slot.setBlocked(true);
        slot.setAvailable(false);
        scheduleTimeRepository.save(slot);
        return true;
    }

    @Transactional
    public void unblock(UUID slotId) {
        ScheduleTime slot = scheduleTimeRepository.findById(slotId).orElseThrow(NotFoundException::new);
        if (!Boolean.TRUE.equals(slot.getBlocked())) {
            return;
        }
        slot.setBlocked(false);
        slot.setAvailable(true);
        scheduleTimeRepository.save(slot);
    }

    /** Ends a hold early and passes the slot to the next person, or back on sale. */
    @Transactional
    public void releaseHold(UUID slotId) {
        waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED).stream()
                .filter(entry -> entry.getOfferedSlot() != null && entry.getOfferedSlot().getId().equals(slotId))
                .findFirst()
                .ifPresent(entry -> waitlistService.release(entry, WaitlistStatus.WAITING));
    }

    /** The side panel for one slot. */
    public record SlotPanel(UUID slotId,
                            String heading,
                            String day,
                            ScheduleBoard.Cell cell,
                            String appointmentLink,
                            String holdLine,
                            int waitingWhoFit,
                            boolean canBook,
                            boolean canOffer,
                            boolean canBlock,
                            boolean canUnblock,
                            boolean canRelease) {

        public boolean hasActions() {
            return canBook || canOffer || canBlock || canUnblock || canRelease;
        }
    }
}
