package com.vegs.mediconnect.datasource.staff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Somebody who works at the clinic.
 *
 * The back office had no sign-in at all: anyone who could reach the URL
 * could read every patient's record and cancel anyone's appointment. This
 * is the smallest thing that fixes that — an email, a hashed password and
 * a role.
 */
@Entity
@Table(name = "staff_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class StaffUser {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt. Never the password itself, not even in the demo seed. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role;

    /** Which clinic they work at, shown under their name. */
    @Column
    private String clinicCode;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * The doctor this login belongs to, for a clinician. It is what lets a
     * doctor change their own availability and days off, and nobody
     * else's. Null for the front desk, and for a clinician not yet linked.
     */
    @Column(columnDefinition = "UUID")
    private java.util.UUID doctorId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    /** The initials shown in the side navigation. */
    public String getInitials() {
        if (name == null || name.isBlank()) {
            return role == null ? "" : role.getInitials();
        }
        String[] parts = name.trim().split("\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() < 2 && !part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.toString();
    }
}
