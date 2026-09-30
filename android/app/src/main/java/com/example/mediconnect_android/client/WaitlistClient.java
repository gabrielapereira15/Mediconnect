package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.WaitlistEntry;

import java.util.List;

public interface WaitlistClient {

    /** Every waitlist this patient is on, newest first. */
    List<WaitlistEntry> list(String email);

    /** Gives up a place in one queue. */
    boolean leave(String email, String entryId);

    /**
     * "Take it": books the held slot in place of the visit it replaces.
     * Throws ApiException with the server's words when the offer has ended.
     */
    com.example.mediconnect_android.model.Appointment accept(String email, String entryId);

    /** "Keep mine": passes the held slot on; they stay on the list. */
    boolean decline(String email, String entryId);

    /**
     * Asks to be told if something earlier opens up with this doctor.
     *
     * The date of the appointment they already hold goes with it, so the
     * clinic only offers slots that are actually an improvement; from a
     * full day on the booking screen it is that day, and the server works
     * out that they hold nothing there and offers it too. Throws
     * ApiException with status 409 when they are already on this list.
     */
    boolean join(String email, String doctorId, String currentAppointmentDate);

    /**
     * Profile's "Earlier-slot offers" switch as the clinic has it: true or
     * false, or null when it could not be asked.
     */
    Boolean offersOn(String email);

    /**
     * Turns earlier-slot offers on or off. Off pauses them; the patient
     * stays on every waitlist. Returns the new setting, or null when the
     * clinic did not take it.
     */
    Boolean setOffersOn(String email, boolean on);
}
