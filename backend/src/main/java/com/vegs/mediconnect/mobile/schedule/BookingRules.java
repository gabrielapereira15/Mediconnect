package com.vegs.mediconnect.mobile.schedule;

import com.vegs.mediconnect.datasource.schedule.ScheduleTime;

import java.time.LocalDateTime;

/**
 * When a slot may be booked.
 *
 * The directory says when a doctor is next free, the booking screen lists
 * the times, and the booking endpoint accepts one. All three have to agree:
 * when they did not, the doctor list advertised "Today 3:30 PM" at quarter
 * past three and the slot picker then showed nothing for today.
 */
public final class BookingRules {

    /** How much notice the clinic needs before an appointment. */
    public static final int LEAD_HOURS = 6;

    private BookingRules() {
    }

    /** Free, and far enough ahead that the clinic can prepare for it. */
    public static boolean isBookable(ScheduleTime scheduleTime) {
        return Boolean.TRUE.equals(scheduleTime.getAvailable()) && hasEnoughNotice(scheduleTime);
    }

    /** Far enough ahead, whether or not anyone has taken it. */
    public static boolean hasEnoughNotice(ScheduleTime scheduleTime) {
        return scheduleTime.getDateTime().isAfter(LocalDateTime.now().plusHours(LEAD_HOURS));
    }
}
