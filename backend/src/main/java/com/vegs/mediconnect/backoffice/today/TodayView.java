package com.vegs.mediconnect.backoffice.today;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.patient.Patient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One day, as the front desk needs to see it (board B01).
 *
 * Everything here is derived rather than stored. A row's state is what its
 * facts already say — cancelled, checked in, in the past, form in or not —
 * so there is no second copy of the truth to drift from the first.
 */
public final class TodayView {

    private TodayView() {
    }

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH);

    /** Where an appointment has got to, in the words staff use. */
    public enum State {
        BOOKED("Booked", "mc-badge-info"),
        CHECKED_IN("Checked in", "mc-badge-brand"),
        COMPLETED("Completed", "mc-badge-success"),
        CANCELLED("Cancelled", "mc-badge-danger"),
        NO_SHOW("No show", "mc-badge-warning");

        private final String label;
        private final String badgeClass;

        State(String label, String badgeClass) {
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

    /** One line of the queue. */
    public record Row(UUID id,
                      String time,
                      String patientName,
                      String patientInitials,
                      String patientBirthdate,
                      String bookedForName,
                      int allergyCount,
                      String doctorName,
                      boolean formIn,
                      State state,
                      String waitingFor,
                      boolean past) {

        /** "2 allergies" is worth a colour; nothing on file is not. */
        public boolean hasAllergies() {
            return allergyCount > 0;
        }

        public boolean isCheckedIn() {
            return state == State.CHECKED_IN;
        }

        public boolean canCheckIn() {
            return state == State.BOOKED && !past;
        }
    }

    /** How a doctor's day is filling. */
    public record DoctorLoad(String name, int booked, int total) {
        public int percent() {
            return total == 0 ? 0 : Math.round(booked * 100f / total);
        }
    }

    /** The numbers across the top. */
    public record Counts(int total,
                         int stillToCome,
                         int checkedIn,
                         int waitingNow,
                         String longestWait,
                         int formsPending,
                         int formsDueSoon,
                         int freedSlots) {
    }

    // ---- building the rows -------------------------------------------------

    public static State stateOf(Appointment appointment, LocalDateTime now) {
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            return State.CANCELLED;
        }
        if (appointment.getCheckedInAt() != null) {
            // Still "checked in" until the visit's own time has passed:
            // a patient in the waiting room is not a completed visit.
            return appointment.getDateTime().isBefore(now) ? State.COMPLETED : State.CHECKED_IN;
        }
        if (appointment.getDateTime().isBefore(now)) {
            // Nobody marked them as arrived and the time has gone. The
            // clinic may simply not use check-in, so this is the softest
            // reading that is still useful.
            return State.NO_SHOW;
        }
        return State.BOOKED;
    }

    public static Row toRow(Appointment appointment, int allergyCount, LocalDateTime now) {
        Patient patient = appointment.getPatient();
        String name = (patient.getFirstName() + " " + patient.getLastName()).trim();

        return new Row(
                appointment.getId(),
                appointment.getScheduleTime().getTime().format(TIME),
                name,
                initials(name),
                patient.getBirthdate(),
                appointment.getBookedForName(),
                allergyCount,
                doctorName(appointment),
                appointment.getFormSubmittedAt() != null,
                stateOf(appointment, now),
                waitingFor(appointment.getCheckedInAt()),
                appointment.getDateTime().isBefore(now));
    }

    /** "12 min", counted from when they said they had arrived. */
    private static String waitingFor(OffsetDateTime checkedInAt) {
        if (checkedInAt == null) {
            return null;
        }
        long minutes = Duration.between(checkedInAt, OffsetDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    private static String doctorName(Appointment appointment) {
        var doctor = appointment.getDoctor();
        return "Dr. " + doctor.getLastName();
    }

    private static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (out.length() < 2 && !part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    // ---- the numbers --------------------------------------------------------

    public static Counts count(List<Row> rows, List<Appointment> appointments,
                               LocalDateTime now, int freedSlots) {
        int total = 0;
        int stillToCome = 0;
        int checkedIn = 0;
        int waitingNow = 0;
        int formsPending = 0;
        int formsDueSoon = 0;
        long longestWaitMinutes = 0;

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.state() == State.CANCELLED) {
                continue;
            }
            total++;

            if (!row.past()) {
                stillToCome++;
            }
            if (row.state() == State.CHECKED_IN) {
                checkedIn++;
                waitingNow++;
                OffsetDateTime at = appointments.get(i).getCheckedInAt();
                if (at != null) {
                    longestWaitMinutes = Math.max(longestWaitMinutes,
                            Duration.between(at, OffsetDateTime.now()).toMinutes());
                }
            }
            if (!row.formIn() && !row.past()) {
                formsPending++;
                LocalDateTime startsAt = appointments.get(i).getDateTime();
                if (startsAt.isBefore(now.plusHours(2))) {
                    formsDueSoon++;
                }
            }
        }

        String longest = longestWaitMinutes <= 0 ? null : longestWaitMinutes + " min";
        return new Counts(total, stillToCome, checkedIn, waitingNow, longest,
                formsPending, formsDueSoon, freedSlots);
    }

    /** Only the times a doctor actually works, so the bar means something. */
    public static LocalTime endOfDay() {
        return LocalTime.of(17, 0);
    }
}
