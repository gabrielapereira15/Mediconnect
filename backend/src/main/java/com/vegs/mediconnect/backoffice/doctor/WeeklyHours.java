package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.shared.Display;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A doctor's usual week (board B06): which days, which hours, how long an
 * appointment is and how far ahead patients may book.
 *
 * The slots patients see are generated from this. Keeping it as one value
 * means the rules — ranges in order, no overlaps, a slot that fits inside
 * its range — are checked in one place, before anything is written.
 */
public record WeeklyHours(Map<DayOfWeek, List<Range>> days, int slotMinutes, int weeks) {

    /** The appointment lengths the desk can choose between. */
    public static final List<Integer> SLOT_LENGTHS = List.of(15, 20, 30, 45, 60);

    /** How far ahead a doctor's diary can be opened. */
    public static final List<Integer> HORIZONS = List.of(1, 2, 3, 4, 6, 8, 12);

    /** The days the form offers, Monday first. */
    public static final List<DayOfWeek> WEEK = List.of(DayOfWeek.values());

    private static final LocalTime EARLIEST = LocalTime.of(6, 0);
    private static final LocalTime LATEST = LocalTime.of(22, 0);

    public record Range(LocalTime start, LocalTime end) {

        public String getLabel() {
            return Display.shortTime(start) + "–" + Display.shortTime(end);
        }
    }

    public WeeklyHours {
        Map<DayOfWeek, List<Range>> copy = new EnumMap<>(DayOfWeek.class);
        days.forEach((day, ranges) -> {
            if (ranges != null && !ranges.isEmpty()) {
                copy.put(day, ranges.stream().sorted(Comparator.comparing(Range::start)).toList());
            }
        });
        days = copy;
    }

    public List<Range> on(DayOfWeek day) {
        return days.getOrDefault(day, List.of());
    }

    public boolean works(DayOfWeek day) {
        return !on(day).isEmpty();
    }

    /** "9:00–12:00 · 13:00–17:00", or "Not available". */
    public String summary(DayOfWeek day) {
        if (!works(day)) {
            return "Not available";
        }
        return on(day).stream().map(Range::getLabel).collect(Collectors.joining(" · "));
    }

    /**
     * The start of every appointment on that day. A slot that would run
     * past the end of its range is not offered: 9:00–12:00 in 45-minute
     * slots ends with 11:15, not 11:45.
     */
    public List<LocalTime> timesOn(DayOfWeek day) {
        List<LocalTime> times = new ArrayList<>();
        Duration step = Duration.ofMinutes(slotMinutes);
        for (Range range : on(day)) {
            LocalTime time = range.start();
            while (!time.plus(step).isAfter(range.end()) && !time.plus(step).isBefore(time)) {
                times.add(time);
                time = time.plus(step);
            }
        }
        return times;
    }

    /** What is wrong with it, in words for the desk; empty when it can be saved. */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (!SLOT_LENGTHS.contains(slotMinutes)) {
            problems.add("Choose an appointment length from the list.");
        }
        if (!HORIZONS.contains(weeks)) {
            problems.add("Choose how far ahead patients can book from the list.");
        }
        for (DayOfWeek day : WEEK) {
            List<Range> ranges = on(day);
            String name = day.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            for (int i = 0; i < ranges.size(); i++) {
                Range range = ranges.get(i);
                if (!range.start().isBefore(range.end())) {
                    problems.add(name + ": a start time must come before its end.");
                } else if (range.start().isBefore(EARLIEST) || range.end().isAfter(LATEST)) {
                    problems.add(name + ": hours must fall between 6:00 and 22:00.");
                } else if (Duration.between(range.start(), range.end()).toMinutes() < slotMinutes) {
                    problems.add(name + ": " + range.getLabel() + " is shorter than one appointment.");
                }
                if (i > 0 && ranges.get(i - 1).end().isAfter(range.start())) {
                    problems.add(name + ": the two stretches overlap.");
                }
            }
        }
        return problems;
    }

    /**
     * Works the usual week out from the slots already in the diary, for a
     * doctor whose hours were never saved: a stretch is a run of times one
     * appointment apart, and a longer gap starts a new one.
     */
    public static WeeklyHours infer(Map<DayOfWeek, List<LocalTime>> seen, int slotMinutes, int weeks) {
        Map<DayOfWeek, List<Range>> days = new HashMap<>();
        Duration step = Duration.ofMinutes(slotMinutes);
        seen.forEach((day, times) -> {
            List<LocalTime> sorted = times.stream().distinct().sorted().toList();
            List<Range> ranges = new ArrayList<>();
            LocalTime start = null;
            LocalTime previous = null;
            for (LocalTime time : sorted) {
                if (start == null) {
                    start = time;
                } else if (Duration.between(previous, time).compareTo(step) > 0) {
                    ranges.add(new Range(start, previous.plus(step)));
                    start = time;
                }
                previous = time;
            }
            if (start != null) {
                ranges.add(new Range(start, previous.plus(step)));
            }
            days.put(day, ranges);
        });
        return new WeeklyHours(days, slotMinutes, weeks);
    }
}
