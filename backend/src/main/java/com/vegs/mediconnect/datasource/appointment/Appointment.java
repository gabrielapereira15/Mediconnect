package com.vegs.mediconnect.datasource.appointment;

import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Appointment {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column
    private String status;

    @Column
    private Boolean canceled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * The person the visit is for, when that is not the account holder —
     * a child or a parent booked by someone else.
     *
     * Held on the appointment rather than as its own patient record: the
     * clinic needs to know who is coming, but these people do not have
     * accounts and nothing else in the app belongs to them. Null means the
     * appointment is for the patient who booked it.
     */
    @Column
    private String bookedForName;

    @Column
    private LocalDate bookedForDateOfBirth;

    @Column
    private String bookedForPhone;

    @Column(columnDefinition = "text")
    private String bookedForNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_time_id", nullable = false)
    private ScheduleTime scheduleTime;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    public LocalDateTime getDateTime() {
        return scheduleTime.getDateTime();
    }

}
