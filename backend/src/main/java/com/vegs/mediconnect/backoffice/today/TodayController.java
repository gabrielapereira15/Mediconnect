package com.vegs.mediconnect.backoffice.today;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Today (board B01).
 *
 * The page the front desk leaves open. It replaces a home screen that was a
 * list of links to entity tables — true of the database and no use to
 * anybody standing at a desk with a patient in front of them.
 */
@Controller
@RequiredArgsConstructor
public class TodayController {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);

    private final AppointmentRepository appointmentRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final HealthEntryRepository healthEntryRepository;
    private final WaitlistEntryRepository waitlistRepository;

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> appointments = appointmentRepository.findAllOnDay(today);
        List<TodayView.Row> rows = new ArrayList<>(appointments.size());
        for (Appointment appointment : appointments) {
            rows.add(TodayView.toRow(appointment, allergyCount(appointment), now));
        }

        // A slot that is free on a day already under way was almost
        // certainly given up by somebody, which is the only kind of free
        // slot worth putting on this page.
        List<ScheduleTime> freed = scheduleTimeRepository.findAll().stream()
                .filter(slot -> slot.getSchedule() != null)
                .filter(slot -> today.equals(slot.getSchedule().getDate()))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .filter(slot -> slot.getDateTime().isAfter(now))
                .toList();

        model.addAttribute("active", "today");
        model.addAttribute("today", today.format(DAY));
        model.addAttribute("rows", rows);
        model.addAttribute("counts", TodayView.count(rows, appointments, now, freed.size()));
        model.addAttribute("doctorLoads", doctorLoads(today));
        model.addAttribute("waitingCount", waitlistRepository
                .findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.WAITING).size());
        return "home/index";
    }

    /**
     * How full each doctor's day is.
     *
     * Counted from the slots the clinic opened rather than from a fixed
     * working day, because that is what "booked out" actually means here.
     */
    private List<TodayView.DoctorLoad> doctorLoads(LocalDate day) {
        Map<String, int[]> byDoctor = new LinkedHashMap<>();

        for (ScheduleTime slot : scheduleTimeRepository.findAll()) {
            var schedule = slot.getSchedule();
            if (schedule == null || !day.equals(schedule.getDate()) || schedule.getDoctor() == null) {
                continue;
            }
            String name = "Dr. " + schedule.getDoctor().getLastName();
            int[] counts = byDoctor.computeIfAbsent(name, key -> new int[2]);
            counts[1]++;
            if (!Boolean.TRUE.equals(slot.getAvailable())) {
                counts[0]++;
            }
        }

        return byDoctor.entrySet().stream()
                .map(entry -> new TodayView.DoctorLoad(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted((a, b) -> Integer.compare(b.percent(), a.percent()))
                .toList();
    }

    /**
     * Allergies only, and only the current ones.
     *
     * It is the one thing on this page that a clinician needs to see before
     * the patient is in front of them, which is why it is worth a query the
     * rest of the record does not get.
     */
    private int allergyCount(Appointment appointment) {
        return (int) healthEntryRepository
                .findAllByPatientAndType(appointment.getPatient(), HealthEntryType.ALLERGY)
                .stream()
                .filter(HealthEntry::isActive)
                .count();
    }
}
