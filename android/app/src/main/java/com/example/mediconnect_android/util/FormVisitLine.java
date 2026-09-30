package com.example.mediconnect_android.util;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.model.Appointment;

import java.util.List;
import java.util.Optional;

/**
 * The line at the top of a form that says which visit it is for.
 *
 * A patient with three visits booked used to land on four questions about
 * "your visit" with nothing to say which one, and could answer for the
 * wrong doctor without knowing it. This names the doctor and the time, and
 * whose visit it is when it was booked for someone else.
 *
 * The visit travels in the fragment's arguments when whoever opens the
 * form has it, since arguments are what survive rotation and the process
 * being killed. A form opened from a message knows only the appointment
 * id, so then the patient's visits are fetched and that one looked up. If
 * it cannot be found the line stays hidden: a form that names no visit is
 * what there was before, and naming the wrong one would be worse.
 */
public final class FormVisitLine {

    private static final String ARG_DOCTOR_NAME = "visitDoctorName";
    private static final String ARG_STARTS_AT = "visitStartsAt";
    private static final String ARG_BOOKED_FOR = "visitBookedFor";

    private FormVisitLine() {
    }

    /** Writes what the line needs from the visit into a fragment's arguments. */
    public static void put(@NonNull Bundle args, @NonNull Appointment appointment) {
        if (appointment.getDoctor() != null) {
            args.putString(ARG_DOCTOR_NAME, appointment.getDoctor().getName());
        }
        args.putString(ARG_STARTS_AT, appointment.getStartsAt());
        args.putString(ARG_BOOKED_FOR, appointment.getBookedForName());
    }

    /** Hands the visit on from one screen's arguments to the next one's. */
    public static void copy(@Nullable Bundle from, @NonNull Bundle to) {
        if (from == null) {
            return;
        }
        for (String key : new String[]{ARG_DOCTOR_NAME, ARG_STARTS_AT, ARG_BOOKED_FOR}) {
            to.putString(key, from.getString(key));
        }
    }

    /**
     * Fills the line in from the arguments, or looks the visit up by id
     * when they do not carry it.
     */
    public static void bind(@NonNull TextView line, @Nullable Bundle args,
                            @Nullable String appointmentId) {
        line.setVisibility(View.GONE);
        if (args != null && show(line, args.getString(ARG_DOCTOR_NAME),
                args.getString(ARG_STARTS_AT), args.getString(ARG_BOOKED_FOR))) {
            return;
        }
        if (appointmentId == null || appointmentId.isEmpty()) {
            return;
        }

        String email = line.getContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");
        AppointmentClient client = new AppointmentClientImpl();

        // Everything the answer touches is the line itself and its own
        // Context, so an answer that arrives after the screen has gone only
        // writes to a view nobody is looking at.
        Background.run(
                () -> find(client.getAppointments(email), appointmentId),
                found -> found.ifPresent(visit -> show(line,
                        visit.getDoctor() == null ? null : visit.getDoctor().getName(),
                        visit.getStartsAt(), visit.getBookedForName())),
                error -> { /* the form works without it; the line stays hidden */ });
    }

    private static Optional<Appointment> find(@Nullable List<Appointment> appointments,
                                              @NonNull String appointmentId) {
        if (appointments == null) {
            return Optional.empty();
        }
        return appointments.stream()
                .filter(a -> a != null && appointmentId.equals(a.getId()))
                .findFirst();
    }

    /**
     * "For your visit with Dr. Robert Chase · Thu 8 Oct, 10:00 AM".
     *
     * Returns whether there was enough to say. The doctor is the part that
     * tells two visits apart, so without one there is no line; without a
     * readable time the doctor alone is still worth saying.
     */
    private static boolean show(@NonNull TextView line, @Nullable String doctorName,
                                @Nullable String startsAt, @Nullable String bookedFor) {
        if (doctorName == null || doctorName.trim().isEmpty()) {
            return false;
        }
        Context context = line.getContext();
        String doctor = WhenLabel.doctorName(doctorName);
        String whose = bookedFor == null || bookedFor.trim().isEmpty()
                ? context.getString(R.string.form_visit_yours, doctor)
                : context.getString(R.string.form_visit_theirs, bookedFor.trim(), doctor);

        line.setText(WhenLabel.parse(startsAt)
                .map(at -> context.getString(R.string.form_visit_when,
                        whose, WhenLabel.whenWords(context, at)))
                .orElse(whose));
        line.setVisibility(View.VISIBLE);
        return true;
    }
}
