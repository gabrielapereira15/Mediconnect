package com.vegs.mediconnect.datasource.notification;

/**
 * What a notification is about.
 *
 * The app shows a different icon and a different thing to do next for each,
 * and a message with no kind is an announcement — which is what every
 * notification was before the waitlist began sending offers.
 */
public final class NotificationKind {

    /** An earlier slot has come free for someone on a waitlist. */
    public static final String WAITLIST_OFFER = "WAITLIST_OFFER";

    /** Something about a visit the patient already holds. */
    public static final String APPOINTMENT = "APPOINTMENT";

    /** Clinic news: opening hours, flu shots, that sort of thing. */
    public static final String ANNOUNCEMENT = "ANNOUNCEMENT";

    private NotificationKind() {
    }
}
