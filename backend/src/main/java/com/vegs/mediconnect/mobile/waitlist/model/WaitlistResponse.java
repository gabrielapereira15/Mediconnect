package com.vegs.mediconnect.mobile.waitlist.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class WaitlistResponse {

    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String status;
    private String currentAppointmentDate;

    /**
     * Whether currentAppointmentDate is a visit they hold, or only the day
     * they asked for when it was full. The app words the offer differently:
     * "instead of your visit" means nothing to someone who has none.
     */
    private boolean holdsVisit;

    private String availableFrom;

    /** The slot held for them, while there is one (status OFFERED). */
    private UUID offeredSlotId;
    private String offeredStartsAt;

    /** When the hold ends, as local ISO date-time. */
    private String offerExpiresAt;
}
