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
 * Either they hold a visit and want it brought forward, or they wanted a
 * day that was already full and hold nothing yet. holdsVisit says which,
 * because the two want different slots.
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
     * The day that sets how late an offer may be: the visit they already
     * hold, or, when they hold none, the full day they asked for. Without
     * it the clinic would offer people appointments later than the ones
     * they have or wanted.
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

    /**
     * The freed slot being held for this patient while they decide.
     *
     * One patient at a time, for a limited time (boards P08 and P13: "We
     * are holding it for you for 1 h 42 min"). Offering a slot to
     * everyone at once meant the fastest thumb won and everybody else
     * opened the app to find it gone.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_slot_id")
    private com.vegs.mediconnect.datasource.schedule.ScheduleTime offeredSlot;

    /** When the hold ends and the slot passes to the next person. */
    @Column
    private OffsetDateTime offerExpiresAt;

    /**
     * The last slot this patient turned down or let lapse, so it is not
     * offered straight back to them when it moves down the list.
     */
    @Column(columnDefinition = "UUID")
    private UUID passedSlotId;

    /**
     * Whether they hold a visit with this doctor on currentAppointmentDate.
     *
     * Joining from a full day on the booking screen sends that day, and
     * such a patient usually holds nothing on it. For them a slot freed on
     * that very day is the one they asked for; for someone who holds a
     * visit there, the same day again is no improvement. Worked out by the
     * server when they join. Null on entries from before this was recorded,
     * which all came from a held visit, so null counts as holding one.
     */
    @Column
    private Boolean holdsVisit;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    /** Whether they hold a visit being improved on; see the field. */
    public boolean holdsVisit() {
        return !Boolean.FALSE.equals(holdsVisit);
    }

    /**
     * The latest day a slot could be on and still be what they asked for.
     *
     * The day before the visit they hold, since the same day again is no
     * improvement; or, holding none, the day they asked for itself.
     */
    public LocalDate lastWantedDate() {
        return holdsVisit() ? currentAppointmentDate.minusDays(1) : currentAppointmentDate;
    }

    /**
     * Whether this entry would be interested in a slot on the given date.
     *
     * Earlier than what they hold (or on the day they asked for, when they
     * hold nothing), not before they are available, and only while they
     * are still on the list.
     *
     * An offer is a reservation now, held for one patient at a time, so
     * someone who turned one down or let it lapse is back to WAITING and
     * eligible for the next slot; only the slot they passed on is skipped,
     * and that check belongs to whoever is choosing who is next.
     */
    public boolean wants(LocalDate slotDate) {
        // Only people still waiting. Somebody already holding an offer has
        // one to decide on, and a slot held for them is not also held for
        // somebody else.
        if (status != WaitlistStatus.WAITING) {
            return false;
        }
        if (slotDate.isAfter(lastWantedDate())) {
            return false;
        }
        return availableFrom == null || !slotDate.isBefore(availableFrom);
    }
}
