package com.example.mediconnect_android.util;

import android.content.Context;

import com.example.mediconnect_android.R;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns a timestamp into the words a card should show.
 *
 * The design system asks for dates that read like speech — "Tomorrow at 2:30
 * PM", "Wed 30 Sep, 10:00 AM" — which means the decision depends on today,
 * and so has to be made on the device rather than baked into a string by the
 * server. Everything here takes the API's ISO timestamp, never its display
 * strings: those change with the server's format and locale, and parsing
 * them back is what broke reminders once already.
 */
public final class WhenLabel {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private WhenLabel() {
    }

    /** Parses an ISO-8601 local date-time, empty rather than throwing. */
    public static Optional<LocalDateTime> parse(String iso) {
        if (iso == null || iso.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(iso));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * "Today, 10:00 AM" · "Tomorrow, 10:00 AM" · "Wed 30 Sep, 10:00 AM".
     * Used for an appointment the patient already holds.
     */
    public static String whenWords(Context context, LocalDateTime at) {
        LocalDate day = at.toLocalDate();
        LocalDate today = LocalDate.now();
        String time = at.format(TIME);

        if (day.isEqual(today)) {
            return context.getString(R.string.when_today, time);
        }
        if (day.isEqual(today.plusDays(1))) {
            return context.getString(R.string.when_tomorrow, time);
        }
        return context.getString(R.string.when_other, day.format(DAY), time);
    }

    /** "10:00 AM" — the time alone, where the day is on its own line. */
    public static String timeWords(LocalDateTime at) {
        return at.format(TIME);
    }

    /**
     * "Today" · "Tomorrow" · "Wed 30 Sep".
     *
     * The companion to {@link #timeWords}: a card that leads with the time
     * says the day underneath, and repeating the date there when the
     * headline already carries it is the kind of duplication that makes a
     * card look machine-filled.
     */
    public static String relativeDayWords(Context context, LocalDateTime at) {
        LocalDate day = at.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.isEqual(today)) {
            return context.getString(R.string.day_today);
        }
        if (day.isEqual(today.plusDays(1))) {
            return context.getString(R.string.day_tomorrow);
        }
        return day.format(DAY);
    }

    /**
     * "Today 2:30 PM" or "Next: Thu 1 Oct" — a doctor's next free slot.
     *
     * Today's slots name the time, because that is what decides whether the
     * patient can make it; a later one names only the day, because the exact
     * minute is chosen on the next screen anyway.
     */
    public static String nextSlotWords(Context context, LocalDateTime at) {
        if (at.toLocalDate().isEqual(LocalDate.now())) {
            return context.getString(R.string.slot_today, at.format(TIME));
        }
        return context.getString(R.string.slot_next, at.format(DAY));
    }

    /** Whether a slot falls today, which decides the badge's colour. */
    public static boolean isToday(LocalDateTime at) {
        return at.toLocalDate().isEqual(LocalDate.now());
    }

    /**
     * Someone's initials for an avatar.
     *
     * Reads the name the way the card shows it, so the directory's
     * "Chase, Robert" gives RC beside "Dr. Robert Chase" — CR next to a
     * spoken name looks like a different person's badge.
     */
    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] parts = spokenOrder(name).split("\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && out.length() < 2) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.toString();
    }

    /**
     * "Dr. Robert Chase" from the API's "Chase, Robert".
     *
     * The design system asks for doctors to read as a patient would say the
     * name, even though the API returns it the way a directory sorts it.
     */
    public static String doctorName(String apiName) {
        if (apiName == null || apiName.isEmpty()) {
            return "";
        }
        String name = spokenOrder(apiName);
        return name.startsWith("Dr.") ? name : "Dr. " + name;
    }

    /** "Chase, Robert" as a person says it: "Robert Chase". */
    private static String spokenOrder(String name) {
        String trimmed = name.trim();
        int comma = trimmed.indexOf(',');
        if (comma <= 0) {
            return trimmed;
        }
        String last = trimmed.substring(0, comma).trim();
        String first = trimmed.substring(comma + 1).trim();
        return (first + " " + last).trim();
    }
}
