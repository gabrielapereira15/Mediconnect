package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * One day of the clinic, doctor by doctor and slot by slot (board B02).
 *
 * Every cell says what that slot is in words as well as colour — booked,
 * checked in, held for someone on the waitlist, free, blocked — because
 * that is what the desk is scanning for, and a colour alone says nothing to
 * a screen reader or to somebody who cannot tell teal from blue.
 */
public final class ScheduleBoard {

    private ScheduleBoard() {
    }

    /** What a slot is, and how it is drawn. */
    public enum State {
        BOOKED("mc-event-info"),
        CHECKED_IN(""),
        DONE("mc-event-done"),
        HELD("mc-event-sun"),
        FREED("mc-event-danger"),
        FREE("mc-event-free"),
        BLOCKED("mc-event-free mc-event-blocked");

        private final String cssClass;

        State(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    /** A slot being held for somebody on the waitlist. */
    public record Hold(String patientName, LocalDateTime until) {
    }

    public record Column(UUID doctorId, String name, String initials, String avatarClass,
                        String specialty, int booked, int total) {
    }

    public record Cell(UUID slotId, State state, String title, String detail) {

        public String getLabel() {
            return title + ", " + detail;
        }
    }

    /** A row of the grid: a time, or a gap in which nobody works. */
    public record Row(String time, List<Cell> cells, String gapLabel) {

        public boolean isGap() {
            return gapLabel != null;
        }
    }

    public record Day(LocalDate date, List<Column> columns, List<Row> rows) {

        public boolean isEmpty() {
            return columns.isEmpty();
        }
    }

    /** One day of a doctor's week: its heading, and how full it is. */
    public record WeekColumn(LocalDate date, String weekday, String dayOfMonth, boolean today,
                             int booked, int total, String note) {

        public boolean isWorking() {
            return total > 0;
        }
    }

    /** One doctor's week, day by day and time by time. */
    public record DoctorWeek(UUID doctorId, String doctorName, String specialty, List<WeekColumn> columns,
                             List<Row> rows, int booked, int total) {

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    /**
     * One doctor's week as a grid: a column per day, a row per time, and
     * the same cells as the day board, so a slot reads the same whichever
     * view it is opened from. Days off and days not worked say so in the
     * column heading rather than leaving a blank column to be puzzled over.
     */
    public static DoctorWeek buildDoctorWeek(Doctor doctor,
                                             List<LocalDate> days,
                                             List<ScheduleTime> slots,
                                             List<Appointment> appointments,
                                             Map<UUID, Hold> holds,
                                             Map<LocalDate, String> daysOff,
                                             LocalDateTime now) {
        Map<UUID, Appointment> booked = new HashMap<>();
        Set<UUID> freed = new java.util.HashSet<>();
        for (Appointment appointment : appointments) {
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                continue;
            }
            UUID slotId = appointment.getScheduleTime().getId();
            if (active(appointment)) {
                booked.put(slotId, appointment);
            } else if (Boolean.TRUE.equals(appointment.getCanceled())) {
                freed.add(slotId);
            }
        }

        Map<LocalDate, Map<LocalTime, ScheduleTime>> grid = new HashMap<>();
        TreeSet<LocalTime> times = new TreeSet<>();
        for (ScheduleTime slot : slots) {
            if (!slot.getSchedule().getDoctor().getId().equals(doctor.getId())
                    || !days.contains(slot.getSchedule().getDate())) {
                continue;
            }
            grid.computeIfAbsent(slot.getSchedule().getDate(), key -> new HashMap<>()).put(slot.getTime(), slot);
            times.add(slot.getTime());
        }

        List<WeekColumn> columns = new ArrayList<>();
        int bookedWeek = 0;
        int totalWeek = 0;
        for (LocalDate day : days) {
            Map<LocalTime, ScheduleTime> onDay = grid.getOrDefault(day, Map.of());
            int total = (int) onDay.values().stream().filter(slot -> !Boolean.TRUE.equals(slot.getBlocked())).count();
            int bookedDay = (int) onDay.values().stream().filter(slot -> booked.containsKey(slot.getId())).count();
            bookedWeek += bookedDay;
            totalWeek += total;
            String note = null;
            if (daysOff.containsKey(day)) {
                String reason = daysOff.get(day);
                note = reason == null || reason.isBlank() ? "Day off" : "Day off \u00b7 " + reason;
            } else if (onDay.isEmpty()) {
                note = "Not working";
            }
            columns.add(new WeekColumn(day,
                    day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                    String.valueOf(day.getDayOfMonth()), day.isEqual(now.toLocalDate()),
                    bookedDay, total, note));
        }

        // The spacing of the slots actually in the diary, as on the day board:
        // slots made before a change of appointment length keep their own.
        // With too few times to tell, the doctor's own length decides.
        Duration step = times.size() >= 3 ? usualStep(times) : Duration.ofMinutes(doctor.slotLength());
        List<Row> rows = new ArrayList<>();
        LocalTime previous = null;
        for (LocalTime time : times) {
            if (previous != null && step != null && Duration.between(previous, time).compareTo(step) > 0) {
                rows.add(new Row(Display.shortTime(previous.plus(step)), List.of(),
                        Display.shortTime(previous.plus(step)) + "\u2013" + Display.shortTime(time)));
            }
            List<Cell> cells = new ArrayList<>();
            for (LocalDate day : days) {
                ScheduleTime slot = grid.getOrDefault(day, Map.of()).get(time);
                cells.add(slot == null ? null
                        : cellFor(slot, booked.get(slot.getId()), freed.contains(slot.getId()),
                        holds.get(slot.getId()), now));
            }
            rows.add(new Row(Display.shortTime(time), cells, null));
            previous = time;
        }

        String name = Display.name(doctor.getFirstName(), doctor.getLastName());
        return new DoctorWeek(doctor.getId(), "Dr. " + name, doctor.getSpecialty(), columns, rows,
                bookedWeek, totalWeek);
    }

    /** A visit that happened, or is still going to. */
    static boolean active(Appointment appointment) {
        return !Boolean.TRUE.equals(appointment.getCanceled())
                && !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
    }

    public static Day build(LocalDate date,
                            List<ScheduleTime> slots,
                            List<Appointment> appointments,
                            Map<UUID, Hold> holds,
                            String specialty,
                            LocalDateTime now) {
        Map<UUID, Appointment> booked = new HashMap<>();
        Set<UUID> freed = new java.util.HashSet<>();
        for (Appointment appointment : appointments) {
            UUID slotId = appointment.getScheduleTime().getId();
            if (active(appointment)) {
                booked.put(slotId, appointment);
            } else if (Boolean.TRUE.equals(appointment.getCanceled())) {
                freed.add(slotId);
            }
        }

        // One column per doctor working that day, in name order.
        Map<UUID, List<ScheduleTime>> byDoctor = new LinkedHashMap<>();
        slots.stream()
                .filter(slot -> date.equals(slot.getSchedule().getDate()))
                .filter(slot -> specialty == null || specialty.isBlank()
                        || specialty.equalsIgnoreCase(slot.getSchedule().getDoctor().getSpecialty()))
                .sorted(Comparator.comparing((ScheduleTime slot) -> slot.getSchedule().getDoctor().getLastName())
                        .thenComparing(ScheduleTime::getTime))
                .forEach(slot -> byDoctor.computeIfAbsent(slot.getSchedule().getDoctor().getId(),
                        key -> new ArrayList<>()).add(slot));

        List<Column> columns = new ArrayList<>();
        TreeSet<LocalTime> times = new TreeSet<>();
        Map<UUID, Map<LocalTime, ScheduleTime>> grid = new HashMap<>();
        for (List<ScheduleTime> doctorSlots : byDoctor.values()) {
            Doctor doctor = doctorSlots.getFirst().getSchedule().getDoctor();
            String name = Display.name(doctor.getFirstName(), doctor.getLastName());
            int total = (int) doctorSlots.stream().filter(slot -> !Boolean.TRUE.equals(slot.getBlocked())).count();
            int bookedCount = (int) doctorSlots.stream().filter(slot -> booked.containsKey(slot.getId())).count();
            columns.add(new Column(doctor.getId(), "Dr. " + doctor.getLastName(), Display.initials(name),
                    Display.avatarClass(name), doctor.getSpecialty(), bookedCount, total));

            Map<LocalTime, ScheduleTime> atTime = new HashMap<>();
            for (ScheduleTime slot : doctorSlots) {
                atTime.put(slot.getTime(), slot);
                times.add(slot.getTime());
            }
            grid.put(doctor.getId(), atTime);
        }

        Duration step = usualStep(times);
        List<Row> rows = new ArrayList<>();
        LocalTime previous = null;
        for (LocalTime time : times) {
            if (previous != null && step != null && Duration.between(previous, time).compareTo(step) > 0) {
                rows.add(new Row(Display.shortTime(previous.plus(step)), List.of(),
                        Display.shortTime(previous.plus(step)) + "–" + Display.shortTime(time)));
            }
            List<Cell> cells = new ArrayList<>();
            for (Column column : columns) {
                ScheduleTime slot = grid.get(column.doctorId()).get(time);
                cells.add(slot == null ? null
                        : cellFor(slot, booked.get(slot.getId()), freed.contains(slot.getId()),
                        holds.get(slot.getId()), now));
            }
            rows.add(new Row(Display.shortTime(time), cells, null));
            previous = time;
        }
        return new Day(date, columns, rows);
    }

    /** The gap between most neighbouring times; anything longer is a break. */
    static Duration usualStep(TreeSet<LocalTime> times) {
        Map<Duration, Integer> seen = new HashMap<>();
        LocalTime previous = null;
        for (LocalTime time : times) {
            if (previous != null) {
                seen.merge(Duration.between(previous, time), 1, Integer::sum);
            }
            previous = time;
        }
        return seen.entrySet().stream()
                .max(Map.Entry.<Duration, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    static Cell cellFor(ScheduleTime slot, Appointment appointment, boolean wasCancelled,
                        Hold hold, LocalDateTime now) {
        boolean over = slot.getDateTime().isBefore(now);
        if (appointment != null) {
            String who = appointment.getBookedForName() != null
                    ? appointment.getBookedForName()
                    : Display.name(appointment.getPatient().getFirstName(), appointment.getPatient().getLastName());
            if (appointment.getCheckedInAt() != null) {
                if (over) {
                    return new Cell(slot.getId(), State.DONE, who, "Completed");
                }
                long waited = Math.max(0, Duration.between(
                        Display.local(appointment.getCheckedInAt()), now).toMinutes());
                return new Cell(slot.getId(), State.CHECKED_IN, who, "Checked in · " + waited + " min");
            }
            if (over) {
                return new Cell(slot.getId(), State.DONE, who, "Not checked in");
            }
            String detail = appointment.getAttendanceConfirmedAt() != null ? "Confirmed" : "Booked";
            if (appointment.getFormSubmittedAt() == null) {
                detail += " · form pending";
            }
            return new Cell(slot.getId(), State.BOOKED, who, detail);
        }
        if (hold != null) {
            return new Cell(slot.getId(), State.HELD, "Held for waitlist",
                    "Offered to " + firstName(hold.patientName()) + " · until "
                            + Display.time(hold.until().toLocalTime()));
        }
        if (Boolean.TRUE.equals(slot.getBlocked())) {
            return new Cell(slot.getId(), State.BLOCKED, "Blocked", "Not bookable");
        }
        if (over) {
            return new Cell(slot.getId(), State.DONE, "Unused", "Nobody booked");
        }
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            return new Cell(slot.getId(), State.BLOCKED, "Unavailable", "Not bookable");
        }
        if (wasCancelled) {
            return new Cell(slot.getId(), State.FREED, "Cancelled", "Back on sale");
        }
        return new Cell(slot.getId(), State.FREE, "Free", "Bookable");
    }

    private static String firstName(String name) {
        return Objects.requireNonNullElse(name, "").split(" ")[0];
    }
}
