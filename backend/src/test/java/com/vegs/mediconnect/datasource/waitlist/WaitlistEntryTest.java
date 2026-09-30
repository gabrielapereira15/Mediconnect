package com.vegs.mediconnect.datasource.waitlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who a freed slot should be offered to.
 *
 * This is the rule that decides whether the waitlist helps or annoys: offer
 * someone a slot later than the one they already hold, or on a day they
 * said they cannot come, and they stop trusting the notifications. Leave
 * out the full day a patient asked for and the waitlist never gives them
 * the one thing they joined it for.
 */
class WaitlistEntryTest {

    private static final LocalDate HELD = LocalDate.of(2026, 10, 20);

    private WaitlistEntry waiting() {
        var entry = new WaitlistEntry();
        entry.setCurrentAppointmentDate(HELD);
        entry.setHoldsVisit(true);
        entry.setStatus(WaitlistStatus.WAITING);
        return entry;
    }

    /** Joined from a full day on the booking screen, holding nothing. */
    private WaitlistEntry askedForTheDay() {
        var entry = waiting();
        entry.setHoldsVisit(false);
        return entry;
    }

    @Test
    @DisplayName("an earlier slot is wanted")
    void earlierSlotIsWanted() {
        assertTrue(waiting().wants(HELD.minusDays(5)));
    }

    @Test
    @DisplayName("a later slot is not")
    void laterSlotIsNotWanted() {
        assertFalse(waiting().wants(HELD.plusDays(1)),
                "offering an appointment later than the one they hold is not an offer");
    }

    @Test
    @DisplayName("the same day is not an improvement")
    void sameDayIsNotWanted() {
        assertFalse(waiting().wants(HELD));
    }

    @Test
    @DisplayName("holding no visit, a slot on the day they asked for is wanted")
    void askedForDayIsWanted() {
        // They joined because that day was full; a slot freeing up on it is
        // exactly what they were waiting for.
        assertTrue(askedForTheDay().wants(HELD));
    }

    @Test
    @DisplayName("holding no visit, a sooner day is wanted and a later one is not")
    void askedForDayOrSooner() {
        var entry = askedForTheDay();

        assertTrue(entry.wants(HELD.minusDays(5)));
        assertFalse(entry.wants(HELD.plusDays(1)),
                "a day later than the one they asked for is not what they asked for");
    }

    @Test
    @DisplayName("an entry from before this was recorded counts as holding a visit")
    void unrecordedCountsAsHolding() {
        var entry = waiting();
        entry.setHoldsVisit(null);

        // Every entry made before the app could join from a full day came
        // from a visit the patient held.
        assertTrue(entry.holdsVisit());
        assertFalse(entry.wants(HELD));
        assertTrue(entry.wants(HELD.minusDays(1)));
    }

    @Test
    @DisplayName("the last wanted day depends on whether they hold a visit")
    void lastWantedDate() {
        assertEquals(HELD.minusDays(1), waiting().lastWantedDate());
        assertEquals(HELD, askedForTheDay().lastWantedDate());
    }

    @Test
    @DisplayName("a slot before they are available is not wanted")
    void slotBeforeAvailabilityIsNotWanted() {
        var entry = waiting();
        entry.setAvailableFrom(HELD.minusDays(3));

        assertFalse(entry.wants(HELD.minusDays(10)),
                "they said they cannot come before this date");
        assertTrue(entry.wants(HELD.minusDays(2)));
    }

    @Test
    @DisplayName("someone with a slot held for them is not offered a second one")
    void holdingAnOfferIsNotWantingAnother() {
        var entry = waiting();
        entry.setStatus(WaitlistStatus.OFFERED);

        // An offer is a reservation: holding two slots for one person keeps
        // one of them from everybody else for nothing.
        assertFalse(entry.wants(HELD.minusDays(5)));
    }

    @Test
    @DisplayName("someone who took an earlier slot is off the list")
    void bookedIsNotWanted() {
        var entry = waiting();
        entry.setStatus(WaitlistStatus.BOOKED);

        assertFalse(entry.wants(HELD.minusDays(5)));
    }

    @Test
    @DisplayName("someone who withdrew is left alone")
    void withdrawnIsNotWanted() {
        var entry = waiting();
        entry.setStatus(WaitlistStatus.WITHDRAWN);

        assertFalse(entry.wants(HELD.minusDays(5)));
    }
}
