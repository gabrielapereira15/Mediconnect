package com.example.mediconnect_android.model;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String creationDate;

    /** WAITLIST_OFFER, APPOINTMENT or ANNOUNCEMENT. */
    private String kind;

    /** Whether the patient has seen it. */
    private boolean read;
    /** The visit a reminder is about, when it is about one. */
    private String appointmentId;
    /** Whether that visit's form is still to be filled in. */
    private boolean formPending;
    /** For a waitlist offer: whether it is still being held. */
    private boolean offerOpen;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }


    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    /** "Fill in form" is offered only while there is one to fill in. */
    public boolean hasFormToFill() {
        return formPending && appointmentId != null && !appointmentId.isEmpty();
    }

    public boolean isOfferOpen() {
        return offerOpen;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
