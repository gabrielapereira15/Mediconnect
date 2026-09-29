package com.example.mediconnect_android.model;

/**
 * A place in the queue for an earlier appointment with one doctor.
 *
 * WAITING means the clinic will tell the patient if something opens up;
 * OFFERED means it already has, and there is a slot being held.
 */
public class WaitlistEntry {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_OFFERED = "OFFERED";

    private String id;
    private String doctorId;
    private String doctorName;
    private String status;
    private String currentAppointmentDate;
    private String availableFrom;

    public String getId() {
        return id;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrentAppointmentDate() {
        return currentAppointmentDate;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    /** Still in the queue, whether or not an offer has gone out. */
    public boolean isOpen() {
        return STATUS_WAITING.equals(status) || STATUS_OFFERED.equals(status);
    }

    public boolean isOffered() {
        return STATUS_OFFERED.equals(status);
    }
}
