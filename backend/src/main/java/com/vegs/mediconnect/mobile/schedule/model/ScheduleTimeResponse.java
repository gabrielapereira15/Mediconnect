package com.vegs.mediconnect.mobile.schedule.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ScheduleTimeResponse {

    UUID id;

    /** ISO HH:mm, for the same reason the date is ISO. */
    String time;

    /**
     * Whether a patient can still take this slot.
     *
     * Taken slots are sent too. A day that shows only the four free times
     * left in it looks like a quiet day; the same day showing which of its
     * twelve are gone tells a patient how soon to decide.
     */
    boolean available;

}
