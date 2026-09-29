package com.vegs.mediconnect.datasource.waitlist;

import com.vegs.mediconnect.datasource.doctor.Doctor;
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
 * A patient waiting for an earlier appointment than the one they could get.
 *
 * A cancelled slot is the clinic's most perishable asset: it is revenue and
 * a treated patient if somebody takes it, and nothing at all if the day
 * passes. The waitlist is what turns a cancellation back into a booking
 * without anyone having to telephone down a list.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class WaitlistEntry {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** The doctor they want to see. A waitlist is not useful in general. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * The date they already hold, so an offer is only made when the freed
     * slot is genuinely earlier. Without it the clinic would offer people
     * appointments later than the ones they have.
     */
    @Column(nullable = false)
    private LocalDate currentAppointmentDate;

    /**
     * The earliest date they can actually attend, which is often not
     * tomorrow — someone may be away next week.
     */
    @Column
    private LocalDate availableFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    /** When the clinic last told them a slot had opened up. */
    @Column
    private OffsetDateTime lastOfferedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    /**
     * Whether this entry would be interested in a slot on the given date.
     *
     * Earlier than what they hold, not before they are available, and only
     * while they are still waiting.
     */
    public boolean wants(LocalDate slotDate) {
        if (status != WaitlistStatus.WAITING) {
            return false;
        }
        if (!slotDate.isBefore(currentAppointmentDate)) {
            return false;
        }
        return availableFrom == null || !slotDate.isBefore(availableFrom);
    }
}
