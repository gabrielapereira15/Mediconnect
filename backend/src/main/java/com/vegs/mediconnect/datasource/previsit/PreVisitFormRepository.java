package com.vegs.mediconnect.datasource.previsit;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreVisitFormRepository extends JpaRepository<PreVisitForm, UUID> {

    Optional<PreVisitForm> findByAppointment(Appointment appointment);
}
