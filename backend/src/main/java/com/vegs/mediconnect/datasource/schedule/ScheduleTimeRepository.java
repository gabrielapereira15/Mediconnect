package com.vegs.mediconnect.datasource.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ScheduleTimeRepository extends JpaRepository<ScheduleTime, UUID> {

    Optional<ScheduleTime> findByTimeAndSchedule(LocalTime time, Schedule schedule);

    /**
     * Every slot in a date range, with its day and doctor already loaded.
     *
     * The schedule board reads the doctor of every cell; left to lazy
     * loading that is a query per slot, and a clinic day has hundreds.
     */
    @Query("""
            select st from ScheduleTime st
            join fetch st.schedule s
            join fetch s.doctor
            where s.date between :from and :to
            order by s.date, st.time
            """)
    List<ScheduleTime> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
