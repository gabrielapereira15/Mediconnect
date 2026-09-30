package com.example.mediconnect_android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.mediconnect_android.model.Appointment;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * The parts of visit reminders that decide what gets set and how it is
 * found again. The alarms themselves need a phone; these do not, and they
 * are where the bugs were: a request code nobody could reproduce, and
 * reminders that outlived the visits they were about.
 */
public class VisitRemindersTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    @Test
    public void aVisitsRequestCodesCanBeWorkedOutAgain() {
        // The old code was the time of setting, so cancelling had nothing
        // to look the alarm up by.
        assertEquals(VisitReminders.requestCode("a1", VisitReminders.DAY_BEFORE),
                VisitReminders.requestCode("a1", VisitReminders.DAY_BEFORE));
        assertEquals(VisitReminders.requestCode("a1", VisitReminders.SOON),
                VisitReminders.requestCode("a1", VisitReminders.SOON));
    }

    @Test
    public void aVisitsTwoAlarmsNeverShareACode() {
        for (String id : Arrays.asList("a1", "demo-a2", "42", "", "8f14e45f-ceea-467a-9a36-0e5c1f3b2d1a")) {
            assertNotEquals(id,
                    VisitReminders.requestCode(id, VisitReminders.DAY_BEFORE),
                    VisitReminders.requestCode(id, VisitReminders.SOON));
        }
    }

    @Test
    public void differentVisitsGetDifferentCodes() {
        assertNotEquals(VisitReminders.requestCode("a1", VisitReminders.DAY_BEFORE),
                VisitReminders.requestCode("a2", VisitReminders.DAY_BEFORE));
    }

    @Test
    public void remindsTheDayBeforeAndHalfAnHourBefore() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 2, 10, 0);

        assertEquals(millis(LocalDateTime.of(2026, 10, 1, 10, 0)),
                VisitReminders.triggerAt(start, VisitReminders.DAY_BEFORE, TORONTO));
        assertEquals(millis(LocalDateTime.of(2026, 10, 2, 9, 30)),
                VisitReminders.triggerAt(start, VisitReminders.SOON, TORONTO));
    }

    @Test
    public void theDayBeforeKeepsTheSameClockTimeWhenTheClocksChange() {
        // Clocks go back in Toronto at 2 AM on Sunday 1 November 2026, so
        // for a visit that morning "the same time yesterday" is 25 hours
        // before, not 24.
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 10, 0);

        long dayBefore = VisitReminders.triggerAt(start, VisitReminders.DAY_BEFORE, TORONTO);

        assertEquals(millis(LocalDateTime.of(2026, 10, 31, 10, 0)), dayBefore);
        assertEquals(Duration.ofHours(25).toMillis(), millis(start) - dayBefore);
    }

    @Test
    public void startPrefersTheIsoTimestamp() {
        Appointment appointment = appointment("a1", "UPCOMING", "2026-10-02T10:00");
        appointment.setDate("Mon, 5 Oct");
        appointment.setTime("3:00 PM");

        assertEquals(Optional.of(LocalDateTime.of(2026, 10, 2, 10, 0)),
                VisitReminders.startOf(appointment, LocalDateTime.of(2026, 9, 30, 12, 0)));
    }

    @Test
    public void startFallsBackToTheDisplayString() {
        Appointment appointment = appointment("a1", "UPCOMING", null);
        appointment.setDate("Fri, 2 Oct");
        appointment.setTime("10:30 AM");

        assertEquals(Optional.of(LocalDateTime.of(2026, 10, 2, 10, 30)),
                VisitReminders.startOf(appointment, LocalDateTime.of(2026, 9, 30, 12, 0)));
    }

    @Test
    public void aDisplayDateAlreadyPastThisYearMeansNextYear() {
        Appointment appointment = appointment("a1", "UPCOMING", null);
        appointment.setDate("Mon, 4 Jan");
        appointment.setTime("9 AM");

        assertEquals(Optional.of(LocalDateTime.of(2027, 1, 4, 9, 0)),
                VisitReminders.startOf(appointment, LocalDateTime.of(2026, 12, 30, 12, 0)));
    }

    @Test
    public void anUnreadableStartIsEmptyRatherThanACrash() {
        Appointment appointment = appointment("a1", "UPCOMING", "next Tuesday");

        assertFalse(VisitReminders.startOf(appointment, LocalDateTime.of(2026, 9, 30, 12, 0))
                .isPresent());
    }

    @Test
    public void onlyUpcomingVisitsKeepTheirReminders() {
        Map<String, Appointment> kept = VisitReminders.upcomingById(Arrays.asList(
                appointment("a1", "UPCOMING", "2026-10-02T10:00"),
                appointment("a2", "CANCELLED", "2026-10-03T10:00"),
                appointment("a3", "COMPLETED", "2026-09-01T10:00"),
                appointment(null, "UPCOMING", "2026-10-04T10:00")));

        assertEquals(Collections.singleton("a1"), kept.keySet());
    }

    @Test
    public void noListMeansNothingIsKept() {
        assertTrue(VisitReminders.upcomingById(null).isEmpty());
    }

    @Test
    public void whatIsStoredReadsBackTheSame() {
        VisitReminders.Entry entry = new VisitReminders.Entry();
        entry.startsAt = "2026-10-02T10:00";
        entry.alarms.add(new VisitReminders.Alarm(VisitReminders.DAY_BEFORE, 1_000L,
                "Appointment tomorrow", "You have an appointment with Dr. Robert Chase tomorrow at 10:00 AM."));
        entry.alarms.add(new VisitReminders.Alarm(VisitReminders.SOON, 2_000L,
                "Appointment soon", "Your appointment with Dr. Robert Chase is in 30 minutes."));

        VisitReminders.Entry read = VisitReminders.decode(VisitReminders.encode(entry));

        assertNotNull(read);
        assertEquals("2026-10-02T10:00", read.startsAt);
        assertEquals(2, read.alarms.size());
        assertEquals(VisitReminders.SOON, read.alarms.get(1).slot);
        assertEquals(2_000L, read.alarms.get(1).at);
        assertEquals("Appointment soon", read.alarms.get(1).title);
        assertEquals("Your appointment with Dr. Robert Chase is in 30 minutes.",
                read.alarms.get(1).body);
    }

    @Test
    public void somethingUnreadableInTheStoreCountsAsNothingSet() {
        assertNull(VisitReminders.decode(null));
        assertNull(VisitReminders.decode(""));
        assertNull(VisitReminders.decode("not json"));
    }

    @Test
    public void anEntryWithNoAlarmsStillReadsAsAList() {
        VisitReminders.Entry read = VisitReminders.decode("{\"startsAt\":\"2026-10-02T10:00\"}");

        assertNotNull(read);
        assertNotNull(read.alarms);
        assertTrue(read.alarms.isEmpty());
    }

    private static long millis(LocalDateTime at) {
        return at.atZone(TORONTO).toInstant().toEpochMilli();
    }

    private static Appointment appointment(String id, String status, String startsAt) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(status);
        appointment.setStartsAt(startsAt);
        return appointment;
    }
}
