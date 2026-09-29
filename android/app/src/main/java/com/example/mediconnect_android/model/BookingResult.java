package com.example.mediconnect_android.model;

/**
 * What came back from trying to book a slot.
 *
 * A plain true/false lost the one thing the patient needs to know when a
 * booking fails: whether to try again later, or pick a different time
 * because somebody else has just taken this one.
 */
public class BookingResult {

    private final boolean booked;
    private final boolean slotTaken;
    private final String message;

    private BookingResult(boolean booked, boolean slotTaken, String message) {
        this.booked = booked;
        this.slotTaken = slotTaken;
        this.message = message;
    }

    public static BookingResult booked() {
        return new BookingResult(true, false, null);
    }

    /** The slot went while the patient was reading the review screen. */
    public static BookingResult slotTaken(String message) {
        return new BookingResult(false, true, message);
    }

    public static BookingResult failed() {
        return new BookingResult(false, false, null);
    }

    public boolean isBooked() {
        return booked;
    }

    public boolean isSlotTaken() {
        return slotTaken;
    }

    /** The clinic's own wording, where it sent any. */
    public String getMessage() {
        return message;
    }
}
