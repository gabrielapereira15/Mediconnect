package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.notification.model.NotificationResponse;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationApiService {

    private final NotificationPatientRepository notificationPatientRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final WaitlistEntryRepository waitlistRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String patientEmail) {
        var patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(PatientNotFoundException::new);

        // Read messages stay on the list. Filtering them out meant that
        // reading one made it disappear, so a patient who glanced at an
        // offer had no way back to what the clinic had actually said.
        var notifications = notificationPatientRepository.findAllByPatientId(patient);
        return notifications.stream()
                .filter(NotificationPatient::notDeleted)
                .sorted(Comparator.comparing(NotificationPatient::getDateCreated).reversed())
                .map(notificationPatient -> NotificationResponse
                        .builder()
                        .id(notificationPatient.getId())
                        .title(notificationPatient.getNotificationId().getTitle())
                        .message(notificationPatient.getNotificationId().getMessage())
                        .kind(kindOf(notificationPatient))
                        .read(Boolean.TRUE.equals(notificationPatient.getAcknowledged()))
                        .archived(Boolean.TRUE.equals(notificationPatient.getArchived()))
                        .appointmentId(notificationPatient.getNotificationId().getAppointmentId())
                        .formPending(formPending(notificationPatient.getNotificationId().getAppointmentId()))
                        .offerOpen(offerOpen(notificationPatient.getNotificationId()))
                        .creationDate(notificationPatient.getDateCreated().toLocalDateTime())
                        .build())
                .toList();
    }

    /**
     * True only for a visit that is still ahead, not cancelled, and has no
     * form in. A reminder about a visit that has since been cancelled keeps
     * its words but loses its button.
     */
    private boolean formPending(UUID appointmentId) {
        if (appointmentId == null) {
            return false;
        }
        return appointmentRepository.findById(appointmentId)
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getFormSubmittedAt() == null)
                .filter(appointment -> appointment.getDateTime().isAfter(java.time.LocalDateTime.now()))
                .isPresent();
    }

    /**
     * Whether the hold a waitlist offer announced is still in place: the
     * same entry, still OFFERED, still holding that slot, and not past its
     * time. An offer message from before offers were linked to their hold
     * is treated as ended rather than risk promising a slot that is gone.
     */
    private boolean offerOpen(com.vegs.mediconnect.datasource.notification.Notification notification) {
        if (!NotificationKind.WAITLIST_OFFER.equals(notification.getKind())
                || notification.getWaitlistEntryId() == null
                || notification.getOfferedSlotId() == null) {
            return false;
        }
        return waitlistRepository.findById(notification.getWaitlistEntryId())
                .filter(entry -> entry.getStatus() == WaitlistStatus.OFFERED)
                .filter(entry -> entry.getOfferedSlot() != null
                        && notification.getOfferedSlotId().equals(entry.getOfferedSlot().getId()))
                .filter(entry -> entry.getOfferExpiresAt() != null
                        && entry.getOfferExpiresAt().isAfter(java.time.OffsetDateTime.now()))
                .isPresent();
    }

    /** A message with no kind of its own is news from the clinic. */
    private String kindOf(NotificationPatient notificationPatient) {
        String kind = notificationPatient.getNotificationId().getKind();
        return kind == null || kind.isBlank() ? NotificationKind.ANNOUNCEMENT : kind;
    }

    /**
     * Marks everything the patient can see as read.
     *
     * One action rather than a tap on each: a list that has built up over a
     * month is not something anyone wants to clear one row at a time.
     */
    @Transactional
    public void markAllRead(String patientEmail) {
        var patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(PatientNotFoundException::new);

        var unread = notificationPatientRepository.findAllByPatientId(patient).stream()
                .filter(NotificationPatient::notDeleted)
                .filter(NotificationPatient::notAck)
                .toList();

        unread.forEach(notificationPatient -> notificationPatient.setAcknowledged(true));
        notificationPatientRepository.saveAll(unread);
    }

    /**
     * Marks one message read, if it is the caller's to mark.
     *
     * The id alone used to be enough, so any signed-in patient could
     * acknowledge a stranger's message by guessing one — the same hole that
     * cancelling an appointment had. Somebody else's is reported as missing
     * rather than forbidden, because "forbidden" confirms the id exists.
     */
    @Transactional
    public void ackNotification(UUID notificationPatientId, String requestingEmail) {
        var notification = owned(notificationPatientId, requestingEmail);
        notification.setAcknowledged(true);
        notificationPatientRepository.save(notification);
    }

    /**
     * Puts one message away. Archiving counts as having dealt with it, so
     * it is marked read too: an archived message should never keep the
     * bell lit.
     */
    @Transactional
    public void archive(UUID notificationPatientId, String requestingEmail) {
        var notification = owned(notificationPatientId, requestingEmail);
        notification.setArchived(true);
        notification.setArchivedAt(java.time.OffsetDateTime.now());
        notification.setAcknowledged(true);
        notificationPatientRepository.save(notification);
    }

    /** Back to the inbox, still read. */
    @Transactional
    public void unarchive(UUID notificationPatientId, String requestingEmail) {
        var notification = owned(notificationPatientId, requestingEmail);
        notification.setArchived(false);
        notification.setArchivedAt(null);
        notificationPatientRepository.save(notification);
    }

    /**
     * "Clear read": archives every message the patient has already read,
     * leaving the inbox with only what still needs them. Returns the ids
     * archived, so the app can offer to undo exactly those.
     */
    @Transactional
    public List<UUID> archiveRead(String patientEmail) {
        var patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(PatientNotFoundException::new);
        var read = notificationPatientRepository.findAllByPatientId(patient).stream()
                .filter(NotificationPatient::notDeleted)
                .filter(delivery -> Boolean.TRUE.equals(delivery.getAcknowledged()))
                .filter(delivery -> !Boolean.TRUE.equals(delivery.getArchived()))
                .toList();
        var now = java.time.OffsetDateTime.now();
        read.forEach(delivery -> {
            delivery.setArchived(true);
            delivery.setArchivedAt(now);
        });
        notificationPatientRepository.saveAll(read);
        return read.stream().map(NotificationPatient::getId).toList();
    }

    /**
     * The message, if it is the caller's.
     *
     * The id alone used to be enough, so any signed-in patient could act on
     * a stranger's message by guessing one. Somebody else's is reported as
     * missing rather than forbidden, because "forbidden" confirms the id
     * exists.
     */
    private NotificationPatient owned(UUID notificationPatientId, String requestingEmail) {
        var notification = notificationPatientRepository.findById(notificationPatientId)
                .orElseThrow(NotificationNotFoundException::new);
        String owner = notification.getPatientId().getEmail();
        if (requestingEmail == null || !requestingEmail.equalsIgnoreCase(owner)) {
            throw new NotificationNotFoundException();
        }
        return notification;
    }

}
