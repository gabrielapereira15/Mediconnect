package com.vegs.mediconnect.backoffice.doctor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A doctor's usual week, and the slots that come out of it (board B06).
 */
class WeeklyHoursTest {

    private static WeeklyHours.Range range(int fromHour, int fromMinute, int toHour, int toMinute) {
        return new WeeklyHours.Range(LocalTime.of(fromHour, fromMinute), LocalTime.of(toHour, toMinute));
    }

    @Test
    @DisplayName("slots run to the end of each stretch, and never past it")
    void slotsFitInside() {
        var hours = new WeeklyHours(Map.of(DayOfWeek.MONDAY, List.of(range(9, 0, 12, 0))), 45, 3);

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(9, 45), LocalTime.of(10, 30), LocalTime.of(11, 15)),
                hours.timesOn(DayOfWeek.MONDAY));
        assertTrue(hours.timesOn(DayOfWeek.TUESDAY).isEmpty(), "no hours, no slots");
    }

    @Test
    @DisplayName("morning and afternoon either side of lunch")
    void twoStretches() {
        var hours = new WeeklyHours(Map.of(DayOfWeek.FRIDAY,
                List.of(range(13, 0, 14, 0), range(9, 0, 10, 0))), 30, 3);

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(13, 0), LocalTime.of(13, 30)),
                hours.timesOn(DayOfWeek.FRIDAY), "stretches are put in order whatever order they came in");
        assertEquals("9:00–10:00 · 13:00–14:00", hours.summary(DayOfWeek.FRIDAY));
        assertEquals("Not available", hours.summary(DayOfWeek.SATURDAY));
    }

    @Test
    @DisplayName("what cannot be saved is said in words")
    void problems() {
        assertTrue(new WeeklyHours(Map.of(DayOfWeek.MONDAY, List.of(range(9, 0, 12, 0))), 30, 3)
                .problems().isEmpty());

        var backwards = new WeeklyHours(Map.of(DayOfWeek.MONDAY, List.of(range(12, 0, 9, 0))), 30, 3);
        assertEquals(List.of("Monday: a start time must come before its end."), backwards.problems());

        var overlapping = new WeeklyHours(Map.of(DayOfWeek.TUESDAY,
                List.of(range(9, 0, 12, 0), range(11, 0, 14, 0))), 30, 3);
        assertEquals(List.of("Tuesday: the two stretches overlap."), overlapping.problems());

        var tooShort = new WeeklyHours(Map.of(DayOfWeek.WEDNESDAY, List.of(range(9, 0, 9, 20))), 30, 3);
        assertFalse(tooShort.problems().isEmpty());

        var night = new WeeklyHours(Map.of(DayOfWeek.THURSDAY, List.of(range(21, 0, 23, 0))), 30, 3);
        assertFalse(night.problems().isEmpty());

        var oddLength = new WeeklyHours(Map.of(), 25, 3);
        assertFalse(oddLength.problems().isEmpty());
    }

    @Test
    @DisplayName("an unsaved week is read back off the diary")
    void inferredFromDiary() {
        var seen = Map.of(DayOfWeek.MONDAY, List.of(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                LocalTime.of(13, 0), LocalTime.of(13, 30)));

        var hours = WeeklyHours.infer(seen, 30, 3);

        assertEquals(List.of(range(9, 0, 10, 30), range(13, 0, 14, 0)), hours.on(DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("the form's fields become the week")
    void readsTheForm() {
        var hours = DoctorsController.read(Map.of(
                "slotMinutes", "30", "weeks", "4",
                "on_MONDAY", "on", "start1_MONDAY", "09:00", "end1_MONDAY", "12:00",
                "start2_MONDAY", "13:00", "end2_MONDAY", "17:00",
                // A day with hours typed in but switched off is not worked.
                "start1_SATURDAY", "09:00", "end1_SATURDAY", "12:00"));

        assertEquals(2, hours.on(DayOfWeek.MONDAY).size());
        assertFalse(hours.works(DayOfWeek.SATURDAY));
        assertEquals(4, hours.weeks());
    }
}
