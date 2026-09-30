package com.vegs.mediconnect.datasource.doctor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorDayOffRepository extends JpaRepository<DoctorDayOff, UUID> {

    List<DoctorDayOff> findAllByDoctorOrderByDateAsc(Doctor doctor);

    List<DoctorDayOff> findAllByDoctorAndDateBetween(Doctor doctor, LocalDate from, LocalDate to);

    boolean existsByDoctorAndDate(Doctor doctor, LocalDate date);

    /** Every doctor's days off in a range, for the week view. */
    List<DoctorDayOff> findAllByDateBetween(LocalDate from, LocalDate to);
}
