package com.vegs.mediconnect.datasource.waitlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who a freed slot should be offered to.
 *
 * This is the rule that decides whether the waitlist helps or annoys: offer
 * someone a slot later than the one they already hold, or on a day they
 * said they cannot come, and they stop trusting the notifications.
 */
class WaitlistEntryTest {

    private static final LocalDate HELD = LocalDate.of(2026, 10, 20);

    private WaitlistEntry waiting() {
        var entry = new WaitlistEntry();
        entry.setCurrentAppointmentDate(HELD);
        entry.setStatus(WaitlistStatus.WAITING);
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
    @DisplayName("a slot before they are available is not wanted")
    void slotBeforeAvailabilityIsNotWanted() {
        var entry = waiting();
        entry.setAvailableFrom(HELD.minusDays(3));

        assertFalse(entry.wants(HELD.minusDays(10)),
                "they said they cannot come before this date");
        assertTrue(entry.wants(HELD.minusDays(2)));
    }

    @Test
    @DisplayName("someone already offered a slot is not offered another")
    void alreadyOfferedIsNotWanted() {
        var entry = waiting();
        entry.setStatus(WaitlistStatus.OFFERED);

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
