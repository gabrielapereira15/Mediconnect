package com.vegs.mediconnect.mobile.schedule;

/**
 * Thrown when a patient confirms a slot that is no longer theirs to take:
 * someone booked it first, or it has come too close to now.
 *
 * The booking screen shows taken slots so a patient can see how a day is
 * filling up, which makes it easy to tap one; and two patients can reach the
 * same free slot seconds apart. Neither is a reason to let the second
 * booking through.
 */
public class SlotNoLongerAvailableException extends RuntimeException {

    public SlotNoLongerAvailableException(String message) {
        super(message);
    }
}
