package com.example.mediconnect_android.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Whether a waitlist offer replaces a visit the patient holds.
 *
 * A patient who joined from a full day on the booking screen holds nothing,
 * and an offer that says "instead of" a visit, or "you keep your visit",
 * tells them about an appointment they never had. The flag is read the way
 * the app reads it from the server, through Gson.
 */
public class WaitlistEntryTest {

    private static WaitlistEntry parse(String json) {
        return new Gson().fromJson(json, WaitlistEntry.class);
    }

    @Test
    public void joinedFromAFullDayHoldsNoVisit() {
        var entry = parse("{\"status\":\"OFFERED\",\"currentAppointmentDate\":\"2026-10-20\","
                + "\"holdsVisit\":false}");

        assertFalse(entry.holdsVisit());
    }

    @Test
    public void joinedFromAVisitHoldsIt() {
        var entry = parse("{\"status\":\"OFFERED\",\"currentAppointmentDate\":\"2026-10-20\","
                + "\"holdsVisit\":true}");

        assertTrue(entry.holdsVisit());
    }

    @Test
    public void aServerThatDoesNotSayMeansAHeldVisit() {
        // Before the server sent the flag, every entry came from a visit the
        // patient held, so its absence keeps the wording it always had.
        var entry = parse("{\"status\":\"OFFERED\",\"currentAppointmentDate\":\"2026-10-20\"}");

        assertTrue(entry.holdsVisit());
    }
}
