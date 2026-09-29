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

}
