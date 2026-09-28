package com.vegs.mediconnect.backoffice.notification;

import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public List<NotificationDTO> findAll() {
        final List<Notification> notifications = notificationRepository.findAll(Sort.by("id"));
        return notifications.stream()
                .filter(Notification::notDeleted)
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .toList();
    }

    @Transactional
    public NotificationDTO get(final UUID id) {
        return notificationRepository.findById(id)
                .map(notification -> mapToDTO(notification, new NotificationDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final NotificationDTO notificationDTO) {
        final Notification notification = new Notification();
        mapToEntity(notificationDTO, notification);
        notification.setIsDeleted(false);
        var createdNotification = notificationRepository.save(notification);
        List<Patient> patients = new ArrayList<>();
        if (Boolean.TRUE.equals(createdNotification.getSendAllPatients())) {
            patients.addAll(patientRepository.findAll());
        } else {
            Optional.ofNullable(notificationDTO.getPatients())
                    .ifPresent(patientsDTO -> {
                        var allIds = patientsDTO.stream().map(UUID::fromString).toList();
                        patients.addAll(patientRepository.findAllById(allIds));
                    });

        }
        patients.forEach(patient -> sendNotificationToPatient(patient, createdNotification));
        return createdNotification.getId();
    }

    private void sendNotificationToPatient(Patient patient, Notification createdNotification) {
        var notificationPatient = new NotificationPatient();
        notificationPatient.setNotificationId(createdNotification);
        notificationPatient.setPatientId(patient);
        notificationPatient.setAcknowledged(Boolean.FALSE);
        notificationPatientRepository.save(notificationPatient);
    }

    public void delete(final UUID id) {
        var notification = notificationRepository.findById(id)
                        .orElseThrow(NotFoundException::new);
        notification.setIsDeleted(true);
        notificationRepository.save(notification);
    }

    private NotificationDTO mapToDTO(final Notification notification,
            final NotificationDTO notificationDTO) {
        notificationDTO.setId(notification.getId());
        notificationDTO.setTitle(notification.getTitle());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setSendAllPatients(notification.getSendAllPatients());
        var notificationPatients = notificationPatientRepository.findAllByNotificationId(notification);
        notificationDTO.setEmails(notificationPatients
                .stream()
                .map(NotificationPatient::getPatientId)
                .map(Patient::getEmail)
                .filter(Objects::nonNull)
                .limit(3)
                .collect(Collectors.joining(", "))
        );
        return notificationDTO;
    }

    private Notification mapToEntity(final NotificationDTO notificationDTO,
            final Notification notification) {
        notification.setId(notificationDTO.getId());
        notification.setTitle(notificationDTO.getTitle());
        notification.setMessage(notificationDTO.getMessage());
        notification.setSendAllPatients(notificationDTO.getSendAllPatients());
        return notification;
    }

    public ReferencedWarning getReferencedWarning(final UUID id) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Notification notification = notificationRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        return null;
    }

}
