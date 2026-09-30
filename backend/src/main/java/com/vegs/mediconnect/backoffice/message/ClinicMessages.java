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
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
 * patients, or to everyone booked on a day — now, or at a time the desk
 * chooses. What the waitlist sends by itself is not listed here; it is a
 * conversation with one patient, not something anyone at the desk wrote.
 *
 * A message is in one of four states, read off its fields: a draft
 * (draft = true), scheduled (a time set, not gone yet), sent (sentAt set),
 * or withdrawn (isDeleted). Only a sent message has delivery rows, so only
 * a sent message can be in anybody's inbox.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicMessages {

    public static final int TITLE_MAX = 80;
    public static final int MESSAGE_MAX = 500;

    /** The soonest a message can be scheduled: sooner than this, send it now. */
    static final Duration SOONEST = Duration.ofMinutes(5);

    /** The furthest ahead one can be scheduled. */
    static final Duration FURTHEST = Duration.ofDays(90);

    /** A link in a message from a clinic is how phishing starts; the app shows plain text. */
    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|ca|net|org|io|ly)\\b)");

    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository deliveryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactions;

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

    /** What the desk has written, before it is sent, scheduled or saved. */
    public record Draft(UUID id, Audience audience, UUID doctorId, LocalDate day, String title, String message)
            implements java.io.Serializable {
    }

    public record Line(UUID id, String title, String audience, String stats, String when,
                       boolean draft, boolean scheduled) {
    }

    public record Sent(UUID id, String title, String message, String audience, String when,
                       int delivered, int read, boolean withdrawn, boolean scheduled) {

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
                // Waiting to go first, soonest first; then everything else, newest first.
                .sorted(Comparator.comparing((Notification notification) -> !isScheduled(notification))
                        .thenComparing(notification -> isScheduled(notification)
                                ? notification.getScheduledFor().toEpochSecond()
                                : -sortKey(notification).toEpochSecond()))
                .map(this::line)
                .toList();
    }

    static boolean isScheduled(Notification notification) {
        return !Boolean.TRUE.equals(notification.getDraft())
                && notification.getSentAt() == null
                && notification.getScheduledFor() != null;
    }

    private static OffsetDateTime sortKey(Notification notification) {
        if (notification.getSentAt() != null) {
            return notification.getSentAt();
        }
        return notification.getLastUpdated() != null ? notification.getLastUpdated() : notification.getDateCreated();
    }

    private Line line(Notification notification) {
        if (Boolean.TRUE.equals(notification.getDraft())) {
            return new Line(notification.getId(), notification.getTitle(), audienceOf(notification, List.of()),
                    "Not sent", "Draft", true, false);
        }
        if (isScheduled(notification)) {
            LocalDateTime at = Display.local(notification.getScheduledFor());
            return new Line(notification.getId(), notification.getTitle(), audienceOf(notification, List.of()),
                    "Goes out " + Display.dayAndTime(at), Display.day(at.toLocalDate()), false, true);
        }
        List<NotificationPatient> deliveries = deliveryRepository.findAllByNotificationId(notification);
        long read = deliveries.stream().filter(delivery -> Boolean.TRUE.equals(delivery.getAcknowledged())).count();
        String stats = deliveries.size() > 20
                ? "read by " + Math.round(read * 100f / deliveries.size()) + "%"
                : "read by " + read + " of " + deliveries.size();
        return new Line(notification.getId(), notification.getTitle(), audienceOf(notification, deliveries),
                stats, Display.day(Display.local(sortKey(notification)).toLocalDate()), false, false);
    }

    /** The audience in words, for messages sent before it was recorded too. */
    private static String audienceOf(Notification notification, List<NotificationPatient> deliveries) {
        if (notification.getAudience() != null) {
            return notification.getAudience();
        }
        if (Boolean.TRUE.equals(notification.getSendAllPatients())) {
            return "All patients";
        }
        if (Boolean.TRUE.equals(notification.getDraft())) {
            return "Not chosen yet";
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
        boolean scheduled = isScheduled(notification);
        return new Sent(notification.getId(), notification.getTitle(), notification.getMessage(),
                audienceOf(notification, deliveries),
                Display.dayAndTime(Display.local(scheduled ? notification.getScheduledFor() : sortKey(notification))),
                deliveries.size(), read, !notification.notDeleted(), scheduled);
    }

    @Transactional(readOnly = true)
    public boolean isDraft(UUID id) {
        return notificationRepository.findById(id)
                .map(notification -> Boolean.TRUE.equals(notification.getDraft()))
                .orElseThrow(NotFoundException::new);
    }

    /** A draft, reopened as it was left, audience included. */
    @Transactional(readOnly = true)
    public Draft draft(UUID id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(NotFoundException::new);
        return new Draft(notification.getId(),
                notification.getAudienceKind() == null ? Audience.ALL : Audience.of(notification.getAudienceKind()),
                notification.getAudienceDoctorId(), notification.getAudienceDay(),
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

    /**
     * What stops it being scheduled for that moment; null when it can be.
     *
     * Checked as an instant, not as a wall-clock time: on the night the
     * clocks go back, 1:30 happens twice, and a check on the local time
     * alone could pass a moment that has already gone.
     */
    public static String timingProblem(OffsetDateTime sendAt, OffsetDateTime now) {
        if (sendAt == null) {
            return "Choose when it should go out.";
        }
        if (sendAt.isBefore(now.plus(SOONEST))) {
            return "Choose a time at least five minutes from now, or send it straight away.";
        }
        if (sendAt.isAfter(now.plus(FURTHEST))) {
            return "Messages can be scheduled up to 90 days ahead.";
        }
        return null;
    }

    /** The desk's local time as an instant, the later one when the clocks go back. */
    static OffsetDateTime instant(LocalDateTime local) {
        if (local == null) {
            return null;
        }
        return local.atZone(ZoneId.systemDefault()).withLaterOffsetAtOverlap().toOffsetDateTime();
    }

    /** When a scheduled message will go, and how many it would reach now. */
    public record Planned(OffsetDateTime at, int reach) {
    }

    /** What withdrawing a message turned out to do. */
    public enum Withdrawn { DRAFT_DISCARDED, SCHEDULE_CANCELLED, REMOVED_FROM_INBOXES }

    /** Saves it without sending. Returns its id. */
    @Transactional
    public UUID saveDraft(Draft draft) {
        Notification notification = editable(draft.id());
        notification.setTitle(blankToDash(draft.title()));
        notification.setMessage(blankToDash(draft.message()));
        notification.setKind(NotificationKind.ANNOUNCEMENT);
        notification.setDraft(true);
        notification.setScheduledFor(null);
        notification.setSendAllPatients(false);
        notification.setIsDeleted(false);
        rememberAudience(notification, draft);
        // The list says who a draft is for as soon as that has been chosen.
        boolean chosen = !(draft.audience() == Audience.DOCTOR && draft.doctorId() == null)
                && !(draft.audience() == Audience.DAY && draft.day() == null);
        notification.setAudience(chosen ? audienceLabel(draft) : null);
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

        Notification notification = editable(draft.id());
        fill(notification, draft);
        notification.setDraft(false);
        notification.setScheduledFor(null);
        notification.setSentAt(OffsetDateTime.now());
        Notification saved = notificationRepository.save(notification);
        deliver(saved, recipients);
        return recipients.size();
    }

    /**
     * Sets it to go out at a time the desk chooses. Nobody sees it before
     * then: it gets its delivery rows only when it goes. Returns how many
     * it would reach if it went now; who it reaches is worked out again at
     * the time, so a patient who books that day in the meantime gets it.
     */
    @Transactional
    public Planned schedule(Draft draft, LocalDateTime sendAt) {
        OffsetDateTime at = instant(sendAt);
        String problem = problem(draft);
        if (problem == null) {
            problem = timingProblem(at, OffsetDateTime.now());
        }
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }

        Notification notification = editable(draft.id());
        fill(notification, draft);
        notification.setDraft(false);
        notification.setSentAt(null);
        notification.setScheduledFor(at);
        notificationRepository.save(notification);
        return new Planned(at, recipients(draft).size());
    }

    /** A scheduled message back to a draft, to be changed or sent another time. */
    @Transactional
    public UUID unschedule(UUID id) {
        if (notificationRepository.moveBackToDraft(id) == 0) {
            // Gone out, cancelled, or never scheduled: nothing to move back.
            throw new IllegalStateException("It has already gone out, or it was cancelled, so it cannot be edited.");
        }
        return id;
    }

    /** How many people it would reach, for the line under the buttons. */
    @Transactional(readOnly = true)
    public int reach(Draft draft) {
        if (draft.audience() == Audience.DOCTOR && draft.doctorId() == null
                || draft.audience() == Audience.DAY && draft.day() == null) {
            return 0;
        }
        return recipients(draft).size();
    }

    /**
     * Takes a sent message out of patients' inboxes, stops a scheduled one
     * going out, or throws a draft away. Marked, never deleted: what the
     * clinic said, or meant to say, stays on record.
     */
    @Transactional
    public Withdrawn withdraw(UUID id) {
        if (notificationRepository.markWithdrawn(id) == 0) {
            throw new NotFoundException();
        }
        // Read after the update, so a message the dispatcher sent a moment
        // ago is reported as removed from inboxes, not as "nobody got it".
        Notification notification = notificationRepository.findById(id).orElseThrow(NotFoundException::new);
        if (Boolean.TRUE.equals(notification.getDraft())) {
            return Withdrawn.DRAFT_DISCARDED;
        }
        return notification.getSentAt() == null ? Withdrawn.SCHEDULE_CANCELLED : Withdrawn.REMOVED_FROM_INBOXES;
    }

    // ---- going out on time ---------------------------------------------------------------

    /**
     * Sends every scheduled message whose time has come.
     *
     * Checked once a minute, so a message set for 9:00 is in inboxes by
     * 9:01 at the latest. Each goes to the group as it stands now.
     */
    @Scheduled(fixedDelay = 60_000)
    public int dispatchDue() {
        return dispatchDue(OffsetDateTime.now());
    }

    /**
     * Each message is claimed with one guarded update and delivered in its
     * own transaction: a Cancel or Edit that lands first wins, the
     * dispatcher never writes back an old copy of the row, and one message
     * that fails does not hold back the others.
     */
    public int dispatchDue(OffsetDateTime now) {
        int sent = 0;
        for (Notification due : notificationRepository.findAllBySentAtIsNullAndScheduledForLessThanEqual(now)) {
            if (!isScheduled(due) || !due.notDeleted()) {
                continue;
            }
            try {
                Boolean delivered = transactions.execute(status -> {
                    if (notificationRepository.claimForSending(due.getId(), now) != 1) {
                        return false;
                    }
                    Draft draft = new Draft(due.getId(), Audience.of(due.getAudienceKind()),
                            due.getAudienceDoctorId(), due.getAudienceDay(), due.getTitle(), due.getMessage());
                    List<Patient> recipients = recipients(draft);
                    deliver(notificationRepository.getReferenceById(due.getId()), recipients);
                    log.info("Sent scheduled message {} to {} patient(s)", due.getId(), recipients.size());
                    return true;
                });
                if (Boolean.TRUE.equals(delivered)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.error("Could not send scheduled message {}; it will be tried again", due.getId(), e);
            }
        }
        return sent;
    }

    // ---- pieces --------------------------------------------------------------------------

    /**
     * A new message, or a draft claimed for this change. A scheduled message
     * is changed by moving it back to drafts first, so the dispatcher and
     * the desk never both hold it.
     */
    private Notification editable(UUID id) {
        if (id == null) {
            return new Notification();
        }
        if (notificationRepository.claimDraft(id) != 1) {
            throw new IllegalArgumentException("That message is no longer a draft: it has been sent, "
                    + "scheduled or discarded in the meantime.");
        }
        return notificationRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    private void fill(Notification notification, Draft draft) {
        notification.setTitle(draft.title().trim());
        notification.setMessage(draft.message().trim());
        notification.setKind(NotificationKind.ANNOUNCEMENT);
        notification.setSendAllPatients(draft.audience() == Audience.ALL);
        notification.setAudience(audienceLabel(draft));
        notification.setIsDeleted(false);
        rememberAudience(notification, draft);
    }

    private static void rememberAudience(Notification notification, Draft draft) {
        notification.setAudienceKind(draft.audience().name());
        notification.setAudienceDoctorId(draft.audience() == Audience.DOCTOR ? draft.doctorId() : null);
        notification.setAudienceDay(draft.audience() == Audience.DAY ? draft.day() : null);
    }

    private void deliver(Notification notification, List<Patient> recipients) {
        for (Patient patient : recipients) {
            var delivery = new NotificationPatient();
            delivery.setNotificationId(notification);
            delivery.setPatientId(patient);
            delivery.setAcknowledged(false);
            deliveryRepository.save(delivery);
        }
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
        return !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
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
