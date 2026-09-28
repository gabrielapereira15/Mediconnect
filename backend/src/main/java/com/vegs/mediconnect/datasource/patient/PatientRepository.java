package com.vegs.mediconnect.datasource.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    /** Sign-in is by email, and an address typed on a phone may be capitalised. */
    Optional<Patient> findByEmailIgnoreCase(String email);

}
