package com.vegs.mediconnect.datasource.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;


public interface ScheduleTimeRepository extends JpaRepository<ScheduleTime, UUID> {

    Optional<ScheduleTime> findByTimeAndSchedule(LocalTime time, Schedule schedule);
}
