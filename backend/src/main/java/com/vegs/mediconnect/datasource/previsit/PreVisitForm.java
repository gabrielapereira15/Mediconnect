package com.vegs.mediconnect.datasource.previsit;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * What the patient told the clinic before a visit.
 *
 * The app has asked these questions since it was written and then thrown
 * the answers away: the form validated three radio buttons, said "Form
 * Submitted Successfully" and navigated home without sending anything. The
 * doctor read it nowhere. One row per appointment, so the chart can show
 * what was said and when.
 */
@Entity
@Table(name = "pre_visit_form")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PreVisitForm {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    /** Why they are coming, in their own words. */
    @Column(columnDefinition = "text")
    private String reason;

    /**
     * The symptoms they ticked, as the app's own codes joined by commas.
     *
     * Codes rather than the labels a patient saw, so the wording can change
     * without rewriting what is already recorded.
     */
    @Column(columnDefinition = "text")
    private String symptoms;

    /** YES, NO or UNSURE — never a bare boolean, which cannot say "unsure". */
    @Column
    private String hadSurgery;

    @Column
    private String smokes;

    @Column
    private String drinksAlcohol;

    /** Anything else they wanted the doctor to know. */
    @Column(columnDefinition = "text")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
