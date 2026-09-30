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

    /**
     * Why it was cancelled, when the clinic cancelled it.
     *
     * A cancelled row with no reason tells whoever looks at it later
     * nothing about whether the slot went back to the waitlist because the
     * patient could not come or because the doctor was away.
     */
    @Column(columnDefinition = "text")
    private String cancelReason;

    /**
     * When the patient sent their pre-appointment form, or null if they
     * have not.
     *
     * Both the app and the back office ask "is the form in?" about every
     * upcoming visit, and until this existed each screen answered by
     * assuming. One nullable timestamp is the whole answer, and it is the
     * same answer everywhere.
     */
    @Column
    private OffsetDateTime formSubmittedAt;

    /**
     * When the patient told us they had arrived, or null if they have not.
     *
     * The front desk's queue is built from this, so it records the moment
     * rather than a boolean: "waiting 20 minutes" is the thing a receptionist
     * actually needs to see.
     */
    @Column
    private OffsetDateTime checkedInAt;


    public LocalDateTime getDateTime() {
        return scheduleTime.getDateTime();
    }

}
