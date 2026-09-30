package com.vegs.mediconnect.datasource.review;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Review {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private Float score;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
