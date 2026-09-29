package com.vegs.mediconnect.datasource.waitlist;

public enum WaitlistStatus {

    /** On the list, waiting for something earlier to come up. */
    WAITING,

    /** Told about a freed slot and not yet acted on it. */
    OFFERED,

    /** Took an earlier slot, so the entry is done. */
    BOOKED,

    /** Asked to come off the list, or their appointment passed. */
    WITHDRAWN
}
