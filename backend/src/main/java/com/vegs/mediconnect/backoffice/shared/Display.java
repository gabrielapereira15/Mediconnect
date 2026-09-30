package com.vegs.mediconnect.backoffice.shared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * How the back office writes names, days and times.
 *
 * In one place so "Wed 30 Sep" is spelled the same on every board, and so
 * the avatar a patient gets on the Appointments list is the one they get on
 * the Schedule.
 */
public final class Display {

    private Display() {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);

    private static final String[] AVATARS = {"", "mc-avatar-blue", "mc-avatar-sun"};

    /** "Wed 30 Sep". */
    public static String day(LocalDate date) {
        return date == null ? "" : date.format(DAY);
    }

    /** "Wednesday, 30 September". */
    public static String longDay(LocalDate date) {
        return date == null ? "" : date.format(LONG_DAY);
    }

    /** "10:00 AM". */
    public static String time(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    /** "10:00", for the schedule's time column. */
    public static String shortTime(LocalTime time) {
        return time == null ? "" : time.format(SHORT_TIME);
    }

    /** "Wed 30 Sep, 10:00 AM". */
    public static String dayAndTime(LocalDateTime at) {
        return at == null ? "" : day(at.toLocalDate()) + ", " + time(at.toLocalTime());
    }

    /** A stored instant, in the clinic's own time. */
    public static LocalDateTime local(OffsetDateTime at) {
        return at == null ? null : at.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** "Gabriela Pereira": first name first, as people say it. */
    public static String name(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    /** "GP" for Gabriela Pereira; titles are skipped. */
    public static String initials(String name) {
        if (name == null) {
            return "?";
        }
        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            String bare = part.replace(".", "").toLowerCase(Locale.ENGLISH);
            if (bare.isEmpty() || bare.equals("dr")) {
                continue;
            }
            if (out.length() < 2) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    /** The same person always gets the same colour. */
    public static String avatarClass(String name) {
        if (name == null) {
            return AVATARS[0];
        }
        return AVATARS[Math.floorMod(name.hashCode(), AVATARS.length)];
    }
}
