package com.vegs.mediconnect.backoffice.appointment;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Appointments list (board B03), worked out from the appointments
 * themselves.
 *
 * Nothing here is stored: which tab a visit sits under and what its badge
 * says are read off its date, whether it was cancelled, and what the
 * patient has done since booking, so the list cannot disagree with the
 * visit it describes.
 */
public final class AppointmentsView {

    private AppointmentsView() {
    }

    public static final int PAGE_SIZE = 20;

    public enum Tab {
        UPCOMING("Upcoming"), TODAY("Today"), PAST("Past"), CANCELLED("Cancelled");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getKey() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Tab of(String key) {
            for (Tab tab : values()) {
                if (tab.getKey().equalsIgnoreCase(key)) {
                    return tab;
                }
            }
            return UPCOMING;
        }
    }

    /** How far ahead (or back) to look. */
    public enum Range {
        ANY("Any time", 0), WEEK("Within a week", 7), MONTH("Within a month", 31);

        private final String label;
        private final int days;

        Range(String label, int days) {
            this.label = label;
            this.days = days;
        }

        public String getLabel() {
            return label;
        }

        public String getKey() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Range of(String key) {
            for (Range range : values()) {
                if (range.getKey().equalsIgnoreCase(key)) {
                    return range;
                }
            }
            return ANY;
        }
    }

    /** The badge, in the words the desk uses. Never colour alone. */
    public enum Status {
        BOOKED("Booked", "mc-badge-info"),
        CONFIRMED("Confirmed", "mc-badge-success"),
        FORM_PENDING("Form pending", "mc-badge-warning"),
        CHECKED_IN("Checked in", "mc-badge-brand"),
        COMPLETED("Completed", "mc-badge-success"),
        NOT_CHECKED_IN("Not checked in", "mc-badge-warning"),
        CANCELLED("Cancelled", "mc-badge-danger");

        private final String label;
        private final String badgeClass;

        Status(String label, String badgeClass) {
            this.label = label;
            this.badgeClass = badgeClass;
        }

        public String getLabel() {
            return label;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }

    public record Filters(Tab tab, String query, UUID doctorId, Range range,
                          boolean formPending, int page) {

        public Filters {
            query = query == null ? "" : query.trim();
            range = range == null ? Range.ANY : range;
            page = Math.max(0, page);
        }
    }

    public record Row(UUID id,
                      String day,
                      String time,
                      String patientName,
                      String initials,
                      String avatarClass,
                      String patientBirthdate,
                      String bookedForName,
                      String doctorName,
                      Status status) {
    }

    public record Page(List<Row> rows,
                       Map<Tab, Integer> counts,
                       int total,
                       int page,
                       int pages,
                       int from,
                       int to) {

        public int count(Tab tab) {
            return counts.getOrDefault(tab, 0);
        }

        public boolean hasPrevious() {
            return page > 0;
        }

        public boolean hasNext() {
            return page + 1 < pages;
        }
    }

    // ---- rules --------------------------------------------------------------

    /** A visit moved to another time is shown there, not here as well. */
    public static boolean listed(Appointment appointment) {
        return !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
    }

    public static Tab tabOf(Appointment appointment, LocalDate today) {
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            return Tab.CANCELLED;
        }
        LocalDate day = appointment.getDateTime().toLocalDate();
        if (day.isEqual(today)) {
            return Tab.TODAY;
        }
        return day.isBefore(today) ? Tab.PAST : Tab.UPCOMING;
    }

    public static Status statusOf(Appointment appointment, LocalDateTime now) {
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            return Status.CANCELLED;
        }
        boolean over = appointment.getDateTime().isBefore(now);
        if (appointment.getCheckedInAt() != null) {
            return over ? Status.COMPLETED : Status.CHECKED_IN;
        }
        if (over) {
            // The clinic may simply not use check-in; this says what is
            // known rather than guessing "no show".
            return Status.NOT_CHECKED_IN;
        }
        if (appointment.getFormSubmittedAt() == null) {
            return Status.FORM_PENDING;
        }
        return appointment.getAttendanceConfirmedAt() != null ? Status.CONFIRMED : Status.BOOKED;
    }

    /** Everything but the tab, so each tab's count reflects the search. */
    static boolean matches(Appointment appointment, Filters filters, LocalDateTime now) {
        if (!filters.query().isEmpty()) {
            String needle = filters.query().toLowerCase(Locale.ENGLISH);
            String haystack = (patientName(appointment) + " "
                    + (appointment.getBookedForName() == null ? "" : appointment.getBookedForName()) + " "
                    + Display.name(appointment.getDoctor().getFirstName(), appointment.getDoctor().getLastName()))
                    .toLowerCase(Locale.ENGLISH);
            if (!haystack.contains(needle)) {
                return false;
            }
        }
        if (filters.doctorId() != null && !filters.doctorId().equals(appointment.getDoctor().getId())) {
            return false;
        }
        if (filters.range() != Range.ANY) {
            long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(), appointment.getDateTime().toLocalDate()));
            if (days > filters.range().days) {
                return false;
            }
        }
        if (filters.formPending()) {
            boolean pending = !Boolean.TRUE.equals(appointment.getCanceled())
                    && appointment.getFormSubmittedAt() == null
                    && appointment.getDateTime().isAfter(now);
            if (!pending) {
                return false;
            }
        }
        return true;
    }

    public static Page build(List<Appointment> appointments, Filters filters, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        Map<Tab, Integer> counts = new EnumMap<>(Tab.class);
        for (Tab tab : Tab.values()) {
            counts.put(tab, 0);
        }

        List<Appointment> matching = appointments.stream()
                .filter(AppointmentsView::listed)
                .filter(appointment -> matches(appointment, filters, now))
                .toList();
        for (Appointment appointment : matching) {
            counts.merge(tabOf(appointment, today), 1, Integer::sum);
        }

        // Ahead of us soonest first; behind us most recent first.
        Comparator<Appointment> byTime = Comparator.comparing(Appointment::getDateTime);
        boolean backwards = filters.tab() == Tab.PAST || filters.tab() == Tab.CANCELLED;
        List<Appointment> inTab = matching.stream()
                .filter(appointment -> tabOf(appointment, today) == filters.tab())
                .sorted(backwards ? byTime.reversed() : byTime)
                .toList();

        int total = inTab.size();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(filters.page(), pages - 1);
        int from = page * PAGE_SIZE;
        int to = Math.min(total, from + PAGE_SIZE);

        List<Row> rows = inTab.subList(from, to).stream()
                .map(appointment -> toRow(appointment, now))
                .toList();
        return new Page(rows, counts, total, page, pages, total == 0 ? 0 : from + 1, to);
    }

    static String patientName(Appointment appointment) {
        return Display.name(appointment.getPatient().getFirstName(), appointment.getPatient().getLastName());
    }

    static Row toRow(Appointment appointment, LocalDateTime now) {
        String name = patientName(appointment);
        return new Row(
                appointment.getId(),
                Display.day(appointment.getDateTime().toLocalDate()),
                Display.time(appointment.getDateTime().toLocalTime()),
                name,
                Display.initials(name),
                Display.avatarClass(name),
                appointment.getPatient().getBirthdate(),
                appointment.getBookedForName(),
                "Dr. " + appointment.getDoctor().getLastName(),
                statusOf(appointment, now));
    }
}
