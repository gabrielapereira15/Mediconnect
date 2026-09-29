package com.vegs.mediconnect.mobile.notification;

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
                        .creationDate(notificationPatient.getDateCreated().toLocalDateTime())
                        .build())
                .toList();
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

    public void ackNotification(UUID notificationPatientId) {
        var notification = notificationPatientRepository.findById(notificationPatientId)
                .orElseThrow(NotificationNotFoundException::new);

        notification.setAcknowledged(true);
        notificationPatientRepository.save(notification);
    }

}
