package com.vegs.mediconnect.mobile.waitlist;

/**
 * Thrown when a patient acts on a waitlist offer that is no longer theirs:
 * the hold ran out and the slot moved on, or they already answered it.
 *
 * A 409 with the reason, so the app can say the offer ended rather than
 * that something went wrong.
 */
public class OfferEndedException extends RuntimeException {

    public OfferEndedException(String message) {
        super(message);
    }
}
