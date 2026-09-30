package com.example.mediconnect_android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.mediconnect_android.model.Appointment;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Forms screen and the count on Home's Forms tile both come from this
 * split, so a visit in the wrong half is a number on the tile that the
 * screen behind it does not explain — the report that started this.
 */
public class FormSectionsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 12, 0);

    @Test
    public void unsentAndSentFormsGoToTheirOwnSection() {
        Appointment owed = upcoming("owed", "2026-10-02T10:00", null);
        Appointment sent = upcoming("sent", "2026-10-03T10:00", "2026-09-29T20:12");

        FormSections sections = FormSections.split(Arrays.asList(owed, sent), NOW);

        assertEquals(ids("owed"), ids(sections.toFillIn()));
        assertEquals(ids("sent"), ids(sections.sent()));
    }

    @Test
    public void bothSectionsAreSoonestFirst() {
        List<Appointment> appointments = Arrays.asList(
                upcoming("later", "2026-10-20T09:00", null),
                upcoming("sent-later", "2026-10-15T09:00", "2026-09-29T08:00"),
                upcoming("tomorrow", "2026-10-01T14:30", null),
                upcoming("sent-soon", "2026-10-05T09:00", "2026-09-28T08:00"),
                upcoming("next-week", "2026-10-07T08:15", null));

        FormSections sections = FormSections.split(appointments, NOW);

        assertEquals(ids("tomorrow", "next-week", "later"), ids(sections.toFillIn()));
        assertEquals(ids("sent-soon", "sent-later"), ids(sections.sent()));
    }

    @Test
    public void onlyVisitsStillAheadAreListed() {
        Appointment completed = upcoming("completed", "2026-10-02T10:00", null);
        completed.setStatus("COMPLETED");
        Appointment cancelled = upcoming("cancelled", "2026-10-02T11:00", null);
        cancelled.setStatus("CANCELED");
        Appointment alreadyStarted = upcoming("past", "2026-09-30T11:59", null);
        Appointment rightNow = upcoming("now", "2026-09-30T12:00", null);
        Appointment ahead = upcoming("ahead", "2026-09-30T12:01", null);

        FormSections sections = FormSections.split(
                Arrays.asList(completed, cancelled, alreadyStarted, rightNow, ahead), NOW);

        assertEquals(ids("ahead"), ids(sections.toFillIn()));
        assertTrue(sections.sent().isEmpty());
    }

    @Test
    public void aVisitWithNoReadableTimeIsLeftOut() {
        FormSections sections = FormSections.split(Arrays.asList(
                upcoming("missing", null, null),
                upcoming("blank", "", null),
                upcoming("display-string", "Thu, 8 Oct | 10 a.m.", null)), NOW);

        assertTrue(sections.isEmpty());
    }

    @Test
    public void anEmptySubmittedTimeStillCountsAsNotSent() {
        FormSections sections = FormSections.split(Collections.singletonList(
                upcoming("empty", "2026-10-02T10:00", "")), NOW);

        assertEquals(ids("empty"), ids(sections.toFillIn()));
        assertTrue(sections.sent().isEmpty());
    }

    @Test
    public void nothingBookedMeansEmpty() {
        assertTrue(FormSections.split(null, NOW).isEmpty());
        assertTrue(FormSections.split(Collections.emptyList(), NOW).isEmpty());
        assertTrue(FormSections.split(Collections.singletonList(null), NOW).isEmpty());
    }

    @Test
    public void oneSentFormIsNotEmpty() {
        FormSections sections = FormSections.split(Collections.singletonList(
                upcoming("sent", "2026-10-02T10:00", "2026-09-29T20:12")), NOW);

        assertFalse(sections.isEmpty());
        assertTrue(sections.toFillIn().isEmpty());
    }

    private static Appointment upcoming(String id, String startsAt, String formSubmittedAt) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus("UPCOMING");
        appointment.setStartsAt(startsAt);
        appointment.setFormSubmittedAt(formSubmittedAt);
        return appointment;
    }

    private static List<String> ids(String... ids) {
        return Arrays.asList(ids);
    }

    private static List<String> ids(List<Appointment> appointments) {
        return appointments.stream().map(Appointment::getId).collect(Collectors.toList());
    }
}
