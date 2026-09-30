package com.example.mediconnect_android.util;

import com.example.mediconnect_android.model.Appointment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The patient's forms, split into the ones still owed and the ones sent.
 *
 * A form belongs to a visit that has not happened yet: once the visit is
 * over, cancelled or in the past, there is nobody left to read it. Each
 * half is soonest first, because the visit closest to now is the form the
 * doctor will want first.
 *
 * The Home badge counts with this and the Forms screen lists with it, so
 * the number on the tile and the rows behind it cannot disagree. It needs
 * no Context, which lets that rule be tested on its own.
 */
public final class FormSections {

    private static final String UPCOMING = "UPCOMING";

    private final List<Appointment> toFillIn;
    private final List<Appointment> sent;

    private FormSections(List<Appointment> toFillIn, List<Appointment> sent) {
        this.toFillIn = Collections.unmodifiableList(toFillIn);
        this.sent = Collections.unmodifiableList(sent);
    }

    /**
     * Sorts a patient's appointments into the two sections.
     *
     * {@code now} is passed in rather than read here, so the same list
     * always splits the same way in a test.
     */
    public static FormSections split(List<Appointment> appointments, LocalDateTime now) {
        List<Appointment> toFillIn = new ArrayList<>();
        List<Appointment> sent = new ArrayList<>();
        if (appointments == null) {
            return new FormSections(toFillIn, sent);
        }

        for (Appointment appointment : appointments) {
            if (appointment == null || !UPCOMING.equals(appointment.getStatus())) {
                continue;
            }
            // A visit whose time cannot be read is left out rather than
            // guessed at: a form row that cannot say when its visit is
            // would not answer the question this screen exists for.
            Optional<LocalDateTime> at = WhenLabel.parse(appointment.getStartsAt());
            if (!at.isPresent() || !at.get().isAfter(now)) {
                continue;
            }
            if (appointment.isFormSubmitted()) {
                sent.add(appointment);
            } else {
                toFillIn.add(appointment);
            }
        }

        Comparator<Appointment> soonestFirst = Comparator.comparing(
                a -> WhenLabel.parse(a.getStartsAt()).orElse(LocalDateTime.MAX));
        toFillIn.sort(soonestFirst);
        sent.sort(soonestFirst);
        return new FormSections(toFillIn, sent);
    }

    /** Visits still waiting on their form, soonest first. */
    public List<Appointment> toFillIn() {
        return toFillIn;
    }

    /** Visits whose form the doctor already has, soonest first. */
    public List<Appointment> sent() {
        return sent;
    }

    /** No visit ahead at all, so no form to fill in or look back at. */
    public boolean isEmpty() {
        return toFillIn.isEmpty() && sent.isEmpty();
    }
}
