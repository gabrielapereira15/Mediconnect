package com.vegs.mediconnect.datasource.health;

import com.vegs.mediconnect.datasource.patient.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One line of a patient's health record: an allergy, a medication or a
 * condition.
 *
 * Held as a single table with a type rather than three, because from the
 * patient's side they are the same act — "here is something the clinic
 * should know about me" — and every screen that shows one shows all three.
 * The FHIR mapping splits them back out, since AllergyIntolerance,
 * MedicationStatement and Condition are separate resources.
 *
 * Patients type free text; they do not know SNOMED. `code` is therefore
 * nullable and filled in later by the clinic, which is what
 * `CodeableConcept.text` without a coding is for.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class HealthEntry {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthEntryType type;

    /** What the patient called it, e.g. "Penicillin" or "Metformin 500mg". */
    @Column(nullable = false)
    private String description;

    /**
     * A clinical code for the description, once someone has assigned one.
     * Null while the entry is still only the patient's own words.
     */
    @Column
    private String code;

    /** The system `code` belongs to, e.g. SNOMED CT. Null with `code`. */
    @Column
    private String codeSystem;

    /** For allergies: what happens. For conditions: how it presents. */
    @Column(columnDefinition = "text")
    private String note;

    /** Roughly when it started, if the patient knows. */
    @Column
    private LocalDate onsetDate;

    /**
     * False once the patient stops a medication or a problem resolves.
     * Kept rather than deleted: a summary that silently drops a past
     * reaction is worse than one that marks it inactive.
     */
    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}
