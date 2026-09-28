package com.vegs.mediconnect.datasource.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    Optional<Schedule> findByDoctorIdAndDate(UUID doctorId, LocalDate date);

}
