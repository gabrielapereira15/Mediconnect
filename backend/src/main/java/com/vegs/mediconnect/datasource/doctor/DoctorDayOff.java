package com.vegs.mediconnect.datasource.doctor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A day a doctor is not in, though it is one of their usual days (board
 * B06's "Days off"): a conference, leave, a sick day.
 *
 * Its free slots are blocked, never deleted, and any patients already
 * booked that day are listed as needing a new time rather than being
 * moved without anyone telling them.
 */
@Entity
@Getter
@Setter
public class DoctorDayOff {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private String reason;
}
