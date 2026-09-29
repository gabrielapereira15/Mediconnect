package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.WaitlistEntry;

import java.util.List;

public interface WaitlistClient {

    /** Every waitlist this patient is on, newest first. */
    List<WaitlistEntry> list(String email);

    /** Gives up a place in one queue. */
    boolean leave(String email, String entryId);

    /**
     * Asks to be told if something earlier opens up with this doctor.
     *
     * The date of the appointment they already hold goes with it, so the
     * clinic only offers slots that are actually an improvement.
     */
    boolean join(String email, String doctorId, String currentAppointmentDate);
}
