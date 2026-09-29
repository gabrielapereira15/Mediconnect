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
    private final Appointment appointment;

    private BookingResult(boolean booked, boolean slotTaken, String message,
                          Appointment appointment) {
        this.booked = booked;
        this.slotTaken = slotTaken;
        this.message = message;
        this.appointment = appointment;
    }

    /**
     * Booked, with the appointment the clinic created.
     *
     * The id matters: the screen that follows offers the pre-appointment
     * form, and a form has to belong to a visit.
     */
    public static BookingResult booked(Appointment appointment) {
        return new BookingResult(true, false, null, appointment);
    }

    /** The slot went while the patient was reading the review screen. */
    public static BookingResult slotTaken(String message) {
        return new BookingResult(false, true, message, null);
    }

    public static BookingResult failed() {
        return new BookingResult(false, false, null, null);
    }

    /** The appointment as the clinic created it, or null if it was not. */
    public Appointment getAppointment() {
        return appointment;
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
