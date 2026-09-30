package com.vegs.mediconnect.mobile.notification.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private LocalDateTime creationDate;

    /** WAITLIST_OFFER, APPOINTMENT or ANNOUNCEMENT. */
    private String kind;

    /**
     * Whether the patient has seen it.
     *
     * Read messages used to be filtered out of the list altogether, so
     * reading one made it vanish and there was no way back to what the
     * clinic had said. They stay now, and this is what marks them.
     */
    private boolean read;

    /** The visit a reminder is about, when it is about one. */
    private UUID appointmentId;

    /**
     * Whether that visit's form is still to be filled in, so the message
     * can offer "Fill in form" only while there is one to fill in.
     */
    private boolean formPending;

    /**
     * For a waitlist offer: whether it is still being held for this
     * patient. False once it was taken, turned down, passed on or ran out,
     * so the app stops offering "See offer" for something that is gone.
     */
    private boolean offerOpen;

}
