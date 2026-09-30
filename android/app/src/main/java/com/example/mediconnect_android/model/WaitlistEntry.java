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
    /**
     * Whether currentAppointmentDate is a visit they hold, or only the full
     * day they asked for on the booking screen. Boxed so that a server
     * which does not send it reads as holding one, which is all it knew.
     */
    private Boolean holdsVisit;
    private String availableFrom;
    /** The slot held for them while the offer lasts, as local ISO date-time. */
    private String offeredStartsAt;
    /** When the hold ends, as local ISO date-time. */
    private String offerExpiresAt;

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

    public String getOfferedStartsAt() {
        return offeredStartsAt;
    }

    public String getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    /**
     * True when an offer would replace a visit they hold; false when they
     * joined from a full day and hold nothing, so there is no visit to
     * keep or give up and the offer should not speak of one.
     */
    public boolean holdsVisit() {
        return !Boolean.FALSE.equals(holdsVisit);
    }

    /** Still in the queue, whether or not an offer has gone out. */
    public boolean isOpen() {
        return STATUS_WAITING.equals(status) || STATUS_OFFERED.equals(status);
    }

    public boolean isOffered() {
        return STATUS_OFFERED.equals(status);
    }
}
