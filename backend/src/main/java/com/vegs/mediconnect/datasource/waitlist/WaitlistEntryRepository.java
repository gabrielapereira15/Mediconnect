package com.vegs.mediconnect.datasource.waitlist;

import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    /**
     * Everyone waiting on this doctor, longest-waiting first.
     *
     * Order matters: offering a freed slot to whoever asked first is the
     * only rule a clinic can defend to the person who did not get it.
     */
    List<WaitlistEntry> findAllByDoctorAndStatusOrderByDateCreatedAsc(
            Doctor doctor, WaitlistStatus status);

    List<WaitlistEntry> findAllByPatientOrderByDateCreatedDesc(Patient patient);

    boolean existsByPatientAndDoctorAndStatus(
            Patient patient, Doctor doctor, WaitlistStatus status);
}
