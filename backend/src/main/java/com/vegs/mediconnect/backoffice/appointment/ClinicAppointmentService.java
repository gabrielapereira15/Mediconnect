package com.vegs.mediconnect.backoffice.appointment;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.appointment.AppointmentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The front desk's side of an appointment: what the panel shows, where it
 * could move to, and the nudge about an unfilled form.
 *
 * Booking, moving, cancelling and checking in themselves go through the
 * same service the app uses, so the clinic and the patient cannot end up
 * with different rules for the same slot.
 */
@Service
@RequiredArgsConstructor
public class ClinicAppointmentService {

    /** How far ahead a reschedule looks for free times. */
    static final int RESCHEDULE_DAYS = 21;

    private final AppointmentRepository appointmentRepository;
    private final HealthEntryRepository healthEntryRepository;
    private final WaitlistEntryRepository waitlistRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;

    /** One tab of the list, filtered and paged, built while the data is at hand. */
    @Transactional(readOnly = true)
    public AppointmentsView.Page page(AppointmentsView.Filters filters, LocalDateTime now) {
        return AppointmentsView.build(appointmentRepository.findAll(), filters, now);
    }

    /** The same tab, every page of it, as the cells of a spreadsheet. */
    @Transactional(readOnly = true)
    public List<List<String>> export(AppointmentsView.Filters filters, LocalDateTime now) {
        return appointmentRepository.findAll().stream()
                .filter(AppointmentsView::listed)
                .filter(appointment -> AppointmentsView.matches(appointment, filters, now))
                .filter(appointment -> AppointmentsView.tabOf(appointment, now.toLocalDate()) == filters.tab())
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .map(appointment -> java.util.Arrays.asList(
                        appointment.getDateTime().toLocalDate().toString(),
                        Display.time(appointment.getDateTime().toLocalTime()),
                        AppointmentsView.patientName(appointment),
                        appointment.getPatient().getBirthdate(),
                        appointment.getBookedForName(),
                        "Dr. " + Display.name(appointment.getDoctor().getFirstName(),
                                appointment.getDoctor().getLastName()),
                        AppointmentsView.statusOf(appointment, now).getLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentPanel panel(UUID id, boolean deskMayAct) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(AppointmentNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime at = appointment.getDateTime();
        boolean cancelled = Boolean.TRUE.equals(appointment.getCanceled());
        boolean ahead = at.isAfter(now);
        boolean today = at.toLocalDate().isEqual(now.toLocalDate());
        boolean formIn = appointment.getFormSubmittedAt() != null;
        var patient = appointment.getPatient();
        var doctor = appointment.getDoctor();
        String patientName = Display.name(patient.getFirstName(), patient.getLastName());

        List<AppointmentPanel.Badge> badges = new ArrayList<>();
        var status = AppointmentsView.statusOf(appointment, now);
        if (status != AppointmentsView.Status.FORM_PENDING) {
            badges.add(new AppointmentPanel.Badge(status.getLabel(), status.getBadgeClass()));
        }
        if (!cancelled && ahead) {
            badges.add(formIn
                    ? new AppointmentPanel.Badge("Form in", "mc-badge-success")
                    : new AppointmentPanel.Badge("Form pending", "mc-badge-warning"));
        }

        String allergies = healthEntryRepository
                .findAllByPatientAndType(patient, HealthEntryType.ALLERGY).stream()
                .filter(HealthEntry::isActive)
                .map(HealthEntry::getDescription)
                .collect(Collectors.joining(", "));

        String patientLine = joinPresent(" · ",
                patient.getGender(), patient.getBirthdate(), patient.getPhoneNumber());

        String bookedFor = appointment.getBookedForName() == null
                ? "The patient"
                : appointment.getBookedForName()
                + (appointment.getBookedForDateOfBirth() == null ? ""
                : " · born " + appointment.getBookedForDateOfBirth());

        String attendance = appointment.getAttendanceConfirmedAt() == null
                ? "Not confirmed yet"
                : "Confirmed " + Display.day(Display.local(appointment.getAttendanceConfirmedAt()).toLocalDate());
        String form = formIn
                ? "Sent " + Display.dayAndTime(Display.local(appointment.getFormSubmittedAt()))
                : "Not sent yet";

        return new AppointmentPanel(
                appointment.getId(),
                Display.dayAndTime(at),
                badges,
                patient.getId(),
                patientName,
                Display.initials(patientName),
                Display.avatarClass(patientName),
                patientLine,
                allergies.isEmpty() ? null : allergies,
                "Dr. " + Display.name(doctor.getFirstName(), doctor.getLastName())
                        + " · " + doctor.getSpecialty(),
                bookedFor,
                appointment.getDateCreated() == null ? null
                        : Display.day(Display.local(appointment.getDateCreated()).toLocalDate()),
                appointment.getCheckedInAt() == null ? null
                        : Display.time(Display.local(appointment.getCheckedInAt()).toLocalTime()),
                attendance,
                form,
                waitlistLine(appointment),
                appointment.getBookedForNotes(),
                appointment.getCancelReason(),
                cancelled,
                deskMayAct && !cancelled && today && appointment.getCheckedInAt() == null,
                deskMayAct && !cancelled && ahead,
                deskMayAct && !cancelled && ahead && !formIn,
                deskMayAct && !cancelled && ahead);
    }

    /** "Wants earlier · offered Tue 30 Sep, 9:00 AM until 12:43 AM". */
    private String waitlistLine(Appointment appointment) {
        return waitlistRepository.findAllByPatientOrderByDateCreatedDesc(appointment.getPatient())
                .stream()
                .filter(entry -> entry.getDoctor().getId().equals(appointment.getDoctor().getId()))
                .filter(entry -> entry.getStatus() == WaitlistStatus.WAITING
                        || entry.getStatus() == WaitlistStatus.OFFERED)
                .findFirst()
                .map(this::describe)
                .orElse(null);
    }

    private String describe(WaitlistEntry entry) {
        if (entry.getStatus() == WaitlistStatus.OFFERED && entry.getOfferedSlot() != null) {
            return "Wants earlier · holding " + Display.dayAndTime(entry.getOfferedSlot().getDateTime())
                    + " until " + Display.time(Display.local(entry.getOfferExpiresAt()).toLocalTime());
        }
        return "Wants earlier · waiting";
    }

    /**
     * Free times the visit could move to: same doctor, the next three
     * weeks, grouped by day.
     */
    /** One free time to pick, already written out. */
    public record SlotChoice(UUID id, String time) {
    }

    @Transactional(readOnly = true)
    public Map<String, List<SlotChoice>> freeSlotsFor(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        return freeSlots(appointment.getDoctor().getId(), RESCHEDULE_DAYS);
    }

    /** Free, unblocked times with one doctor from now on, grouped by day. */
    @Transactional(readOnly = true)
    public Map<String, List<SlotChoice>> freeSlots(UUID doctorId, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate last = now.toLocalDate().plusDays(days);
        Map<String, List<SlotChoice>> byDay = new LinkedHashMap<>();
        scheduleTimeRepository.findAll().stream()
                .filter(slot -> slot.getSchedule() != null)
                .filter(slot -> slot.getSchedule().getDoctor().getId().equals(doctorId))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .filter(slot -> !Boolean.TRUE.equals(slot.getBlocked()))
                .filter(slot -> slot.getDateTime().isAfter(now))
                .filter(slot -> !slot.getSchedule().getDate().isAfter(last))
                .sorted(Comparator.comparing(ScheduleTime::getDateTime))
                .forEach(slot -> byDay.computeIfAbsent(
                        Display.day(slot.getSchedule().getDate()), key -> new ArrayList<>())
                        .add(new SlotChoice(slot.getId(), Display.time(slot.getTime()))));
        return byDay;
    }

    /** "Dr. Chase · Tue 29 Sep, 10:30 AM", for a slot chosen elsewhere. */
    @Transactional(readOnly = true)
    public java.util.Optional<String> slotLabel(UUID slotId) {
        return scheduleTimeRepository.findById(slotId)
                .map(slot -> "Dr. " + slot.getSchedule().getDoctor().getLastName()
                        + " · " + Display.dayAndTime(slot.getDateTime()));
    }

    /**
     * Sends the patient a message about their unfilled form, carrying the
     * visit so the app offers "Fill in form" straight from it.
     */
    @Transactional
    public String remindAboutForm(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        var notification = new Notification();
        notification.setKind(NotificationKind.APPOINTMENT);
        notification.setTitle("Please fill in your pre-appointment form");
        notification.setMessage("Dr. " + appointment.getDoctor().getLastName()
                + " would like your answers before your visit on "
                + Display.dayAndTime(appointment.getDateTime())
                + ". It takes about three minutes.");
        notification.setAppointmentId(appointment.getId());
        notification.setSendAllPatients(false);
        notification.setIsDeleted(false);
        var saved = notificationRepository.save(notification);

        var delivery = new NotificationPatient();
        delivery.setNotificationId(saved);
        delivery.setPatientId(appointment.getPatient());
        delivery.setAcknowledged(false);
        notificationPatientRepository.save(delivery);
        return appointment.getPatient().getFirstName();
    }

    private static String joinPresent(String separator, String... parts) {
        List<String> present = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                present.add(part);
            }
        }
        return String.join(separator, present);
    }
}
