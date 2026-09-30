package com.vegs.mediconnect.backoffice.message;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Messages the clinic sends to patients' app inbox (board B07).
 *
 * News and reminders written by the desk, to everyone, to one doctor's
 * patients, or to everyone booked on a day. What the waitlist sends by
 * itself is not listed here; it is a conversation with one patient, not
 * something anyone at the desk wrote.
 */
@Service
@RequiredArgsConstructor
public class ClinicMessages {

    public static final int TITLE_MAX = 80;
    public static final int MESSAGE_MAX = 500;

    /** A link in a message from a clinic is how phishing starts; the app shows plain text. */
    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|ca|net|org|io|ly)\\b)");

    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository deliveryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public enum Audience {
        ALL, DOCTOR, DAY;

        public String getKey() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Audience of(String key) {
            for (Audience audience : values()) {
                if (audience.getKey().equalsIgnoreCase(key)) {
                    return audience;
                }
            }
            return ALL;
        }
    }

    /** What the desk has written, before it is sent or saved. */
    public record Draft(UUID id, Audience audience, UUID doctorId, LocalDate day, String title, String message)
            implements java.io.Serializable {
    }

    public record Line(UUID id, String title, String audience, String stats, String when, boolean draft) {
    }

    public record Sent(UUID id, String title, String message, String audience, String when,
                       int delivered, int read, boolean withdrawn) {

        public int getPercentRead() {
            return delivered == 0 ? 0 : Math.round(read * 100f / delivered);
        }
    }

    // ---- reading ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Line> sentList() {
        return notificationRepository.findAll().stream()
                .filter(Notification::notDeleted)
                .filter(notification -> !NotificationKind.WAITLIST_OFFER.equals(notification.getKind()))
                .sorted(Comparator.comparing(ClinicMessages::sortKey).reversed())
                .map(this::line)
                .toList();
    }

    private static OffsetDateTime sortKey(Notification notification) {
        if (notification.getSentAt() != null) {
            return notification.getSentAt();
        }
        return notification.getLastUpdated() != null ? notification.getLastUpdated() : notification.getDateCreated();
    }

    private Line line(Notification notification) {
        boolean draft = Boolean.TRUE.equals(notification.getDraft());
        if (draft) {
            return new Line(notification.getId(), notification.getTitle(), audienceOf(notification, List.of()),
                    "Not sent", "Draft", true);
        }
        List<NotificationPatient> deliveries = deliveryRepository.findAllByNotificationId(notification);
        long read = deliveries.stream().filter(delivery -> Boolean.TRUE.equals(delivery.getAcknowledged())).count();
        String stats = deliveries.size() > 20
                ? "read by " + Math.round(read * 100f / deliveries.size()) + "%"
                : "read by " + read + " of " + deliveries.size();
        return new Line(notification.getId(), notification.getTitle(), audienceOf(notification, deliveries),
                stats, Display.day(Display.local(sortKey(notification)).toLocalDate()), false);
    }

    /** The audience in words, for messages sent before it was recorded too. */
    private static String audienceOf(Notification notification, List<NotificationPatient> deliveries) {
        if (notification.getAudience() != null) {
            return notification.getAudience();
        }
        if (Boolean.TRUE.equals(notification.getSendAllPatients())) {
            return "All patients";
        }
        if (deliveries.size() == 1) {
            Patient patient = deliveries.getFirst().getPatientId();
            return Display.name(patient.getFirstName(), patient.getLastName());
        }
        return deliveries.size() + " patients";
    }

    @Transactional(readOnly = true)
    public Sent sent(UUID id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(NotFoundException::new);
        List<NotificationPatient> deliveries = deliveryRepository.findAllByNotificationId(notification);
        int read = (int) deliveries.stream().filter(delivery -> Boolean.TRUE.equals(delivery.getAcknowledged())).count();
        return new Sent(notification.getId(), notification.getTitle(), notification.getMessage(),
                audienceOf(notification, deliveries),
                Display.dayAndTime(Display.local(sortKey(notification))),
                deliveries.size(), read, !notification.notDeleted());
    }

    @Transactional(readOnly = true)
    public boolean isDraft(UUID id) {
        return notificationRepository.findById(id)
                .map(notification -> Boolean.TRUE.equals(notification.getDraft()))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Draft draft(UUID id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(NotFoundException::new);
        return new Draft(notification.getId(), Audience.ALL, null, null,
                notification.getTitle(), notification.getMessage());
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> doctors() {
        Map<UUID, String> doctors = new LinkedHashMap<>();
        doctorRepository.findAll().stream()
                .sorted(Comparator.comparing(Doctor::getLastName))
                .forEach(doctor -> doctors.put(doctor.getId(), "Dr. " + Display.name(doctor.getFirstName(), doctor.getLastName())));
        return doctors;
    }

    // ---- writing ----------------------------------------------------------------------

    /** What stops it being sent, in words; null when it can go. */
    public static String problem(Draft draft) {
        if (draft.title() == null || draft.title().isBlank()) {
            return "Give the message a title; it is what patients see first.";
        }
        if (draft.title().length() > TITLE_MAX) {
            return "Keep the title to " + TITLE_MAX + " characters.";
        }
        if (draft.message() == null || draft.message().isBlank()) {
            return "Write the message itself.";
        }
        if (draft.message().length() > MESSAGE_MAX) {
            return "Keep the message to " + MESSAGE_MAX + " characters.";
        }
        if (LINK.matcher(draft.title()).find() || LINK.matcher(draft.message()).find()) {
            return "Messages are plain text with no links, so patients can trust that the clinic never "
                    + "sends them somewhere to click.";
        }
        if (draft.audience() == Audience.DOCTOR && draft.doctorId() == null) {
            return "Choose which doctor's patients it is for.";
        }
        if (draft.audience() == Audience.DAY && draft.day() == null) {
            return "Choose the day whose patients it is for.";
        }
        return null;
    }

    /** Saves it without sending. Returns its id. */
    @Transactional
    public UUID saveDraft(Draft draft) {
        Notification notification = draft.id() == null
                ? new Notification()
                : notificationRepository.findById(draft.id())
                .filter(existing -> Boolean.TRUE.equals(existing.getDraft()))
                .orElseThrow(NotFoundException::new);
        notification.setTitle(blankToDash(draft.title()));
        notification.setMessage(blankToDash(draft.message()));
        notification.setKind(NotificationKind.ANNOUNCEMENT);
        notification.setDraft(true);
        notification.setSendAllPatients(false);
        notification.setIsDeleted(false);
        return notificationRepository.save(notification).getId();
    }

    /**
     * Sends it to everyone in the audience, now. Returns how many it
     * reached. Throws with the reason when it cannot be sent as written.
     */
    @Transactional
    public int send(Draft draft) {
        String problem = problem(draft);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        List<Patient> recipients = recipients(draft);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Nobody is in that group, so there is no one to send it to.");
        }

        Notification notification = draft.id() == null
                ? new Notification()
                : notificationRepository.findById(draft.id())
                .filter(existing -> Boolean.TRUE.equals(existing.getDraft()))
                .orElseThrow(NotFoundException::new);
        notification.setTitle(draft.title().trim());
        notification.setMessage(draft.message().trim());
        notification.setKind(NotificationKind.ANNOUNCEMENT);
        notification.setSendAllPatients(draft.audience() == Audience.ALL);
        notification.setAudience(audienceLabel(draft));
        notification.setDraft(false);
        notification.setSentAt(OffsetDateTime.now());
        notification.setIsDeleted(false);
        Notification saved = notificationRepository.save(notification);

        for (Patient patient : recipients) {
            var delivery = new NotificationPatient();
            delivery.setNotificationId(saved);
            delivery.setPatientId(patient);
            delivery.setAcknowledged(false);
            deliveryRepository.save(delivery);
        }
        return recipients.size();
    }

    /** How many people it would reach, for the send button's label. */
    @Transactional(readOnly = true)
    public int reach(Draft draft) {
        if (draft.audience() == Audience.DOCTOR && draft.doctorId() == null
                || draft.audience() == Audience.DAY && draft.day() == null) {
            return 0;
        }
        return recipients(draft).size();
    }

    /**
     * Takes a sent message out of patients' inboxes, or throws a draft
     * away. Marked, never deleted: what the clinic said stays on record.
     */
    @Transactional
    public void withdraw(UUID id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(NotFoundException::new);
        notification.setIsDeleted(true);
        notificationRepository.save(notification);
    }

    List<Patient> recipients(Draft draft) {
        return switch (draft.audience()) {
            case ALL -> patientRepository.findAll();
            case DOCTOR -> appointmentRepository.findAll().stream()
                    .filter(appointment -> appointment.getDoctor().getId().equals(draft.doctorId()))
                    .filter(ClinicMessages::countsAsPatientOf)
                    .map(Appointment::getPatient)
                    .distinct()
                    .toList();
            case DAY -> appointmentRepository.findAllOnDay(draft.day()).stream()
                    .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                    .map(Appointment::getPatient)
                    .distinct()
                    .toList();
        };
    }

    private static boolean countsAsPatientOf(Appointment appointment) {
        return !com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus.REMOVED.getStatus()
                .equals(appointment.getStatus());
    }

    private String audienceLabel(Draft draft) {
        return switch (draft.audience()) {
            case ALL -> "All patients";
            case DOCTOR -> "Patients of Dr. " + doctorRepository.findById(draft.doctorId())
                    .map(Doctor::getLastName).orElse("?");
            case DAY -> "Patients booked on " + Display.day(draft.day());
        };
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
