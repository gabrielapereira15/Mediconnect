package com.vegs.mediconnect.datasource.appointment;

import com.vegs.mediconnect.datasource.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;
import java.util.UUID;


public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findAllByPatient(Patient patient);

    /**
     * Everything the clinic has on one day, in time order.
     *
     * Joined rather than left to lazy loading: the day view reads the
     * patient, the doctor and the slot of every row, and without this that
     * is four queries per appointment.
     */
    @Query("""
            select a from Appointment a
            join fetch a.scheduleTime st
            join fetch st.schedule s
            join fetch a.patient
            join fetch a.doctor
            where s.date = :date
            order by st.time asc
            """)
    List<Appointment> findAllOnDay(@Param("date") LocalDate date);

    /** The same, across a range of days — a week of the schedule. */
    @Query("""
            select a from Appointment a
            join fetch a.scheduleTime st
            join fetch st.schedule s
            join fetch a.patient
            join fetch a.doctor
            where s.date between :from and :to
            order by s.date asc, st.time asc
            """)
    List<Appointment> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

}
