package com.vegs.mediconnect.datasource.health;

import com.vegs.mediconnect.datasource.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HealthEntryRepository extends JpaRepository<HealthEntry, UUID> {

    List<HealthEntry> findAllByPatientOrderByDateCreatedAsc(Patient patient);

    List<HealthEntry> findAllByPatientAndType(Patient patient, HealthEntryType type);
}
