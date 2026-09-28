package com.vegs.mediconnect.datasource.notification;

import com.vegs.mediconnect.datasource.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationPatientRepository extends JpaRepository<NotificationPatient, UUID> {

    List<NotificationPatient> findAllByNotificationId(Notification notification);
    List<NotificationPatient> findAllByPatientId(Patient patient);

}
