package com.example.mediconnect_android.client;

public interface WaitlistClient {

    /**
     * Asks to be told if something earlier opens up with this doctor.
     *
     * The date of the appointment they already hold goes with it, so the
     * clinic only offers slots that are actually an improvement.
     */
    boolean join(String email, String doctorId, String currentAppointmentDate);
}
