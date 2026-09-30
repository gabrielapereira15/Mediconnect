package com.vegs.mediconnect.backoffice.today;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the front desk's queue says about each appointment.
 *
 * The state is derived rather than stored, so these are the only place the
 * rules are written down: a patient in the waiting room is not a completed
 * visit, and a visit whose time has passed with nobody marking them as
 * arrived is not simply "booked" any more.
 */
class TodayViewTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    private static final LocalDateTime NOON = TODAY.atTime(12, 0);

    @Test
    @DisplayName("still ahead of us and nobody has arrived: booked")
    void booked() {
        var appointment = appointmentAt(LocalTime.of(15, 0));

        assertEquals(TodayView.State.BOOKED, TodayView.stateOf(appointment, NOON));
    }

    @Test
    @DisplayName("arrived, and the visit has not started: checked in")
    void checkedIn() {
        var appointment = appointmentAt(LocalTime.of(15, 0));
        appointment.setCheckedInAt(OffsetDateTime.now());

        assertEquals(TodayView.State.CHECKED_IN, TodayView.stateOf(appointment, NOON));
    }

    @Test
    @DisplayName("arrived, and the time has passed: completed")
    void completed() {
        var appointment = appointmentAt(LocalTime.of(9, 0));
        appointment.setCheckedInAt(OffsetDateTime.now().minusHours(3));

        assertEquals(TodayView.State.COMPLETED, TodayView.stateOf(appointment, NOON));
    }

    @Test
    @DisplayName("the time passed and nobody marked them as arrived: no show")
    void noShow() {
        var appointment = appointmentAt(LocalTime.of(9, 0));

        assertEquals(TodayView.State.NO_SHOW, TodayView.stateOf(appointment, NOON));
    }

    @Test
    @DisplayName("cancelled beats everything else")
    void cancelled() {
        var appointment = appointmentAt(LocalTime.of(15, 0));
        appointment.setCanceled(true);
        appointment.setCheckedInAt(OffsetDateTime.now());

        assertEquals(TodayView.State.CANCELLED, TodayView.stateOf(appointment, NOON));
    }

    @Test
    @DisplayName("the counts leave cancelled visits out of the day's total")
    void countsIgnoreCancelled() {
        var booked = appointmentAt(LocalTime.of(15, 0));
        var cancelled = appointmentAt(LocalTime.of(16, 0));
        cancelled.setCanceled(true);

        List<Appointment> appointments = List.of(booked, cancelled);
        List<TodayView.Row> rows = appointments.stream()
                .map(appointment -> TodayView.toRow(appointment, 0, NOON))
                .toList();

        var counts = TodayView.count(rows, appointments, NOON, 0);

        assertEquals(1, counts.total(), "a cancelled visit is not one of today's appointments");
        assertEquals(1, counts.stillToCome());
    }

    @Test
    @DisplayName("a form is only pending while the visit is still ahead")
    void formsPendingOnlyAhead() {
        var ahead = appointmentAt(LocalTime.of(15, 0));
        var past = appointmentAt(LocalTime.of(9, 0));

        List<Appointment> appointments = List.of(ahead, past);
        List<TodayView.Row> rows = appointments.stream()
                .map(appointment -> TodayView.toRow(appointment, 0, NOON))
                .toList();

        var counts = TodayView.count(rows, appointments, NOON, 0);

        // Chasing someone for a form after their visit has been and gone is
        // not something the desk should be prompted to do.
        assertEquals(1, counts.formsPending());
        assertEquals(0, counts.formsDueSoon(), "15:00 is more than two hours from noon");
    }

    // ---- fixtures -----------------------------------------------------------

    private Appointment appointmentAt(LocalTime time) {
        var doctor = new Doctor();
        doctor.setFirstName("Ana");
        doctor.setLastName("Costa");

        var schedule = new Schedule();
        schedule.setDate(TODAY);
        schedule.setDoctor(doctor);

        var slot = new ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(time);
        slot.setSchedule(schedule);

        var patient = new Patient();
        patient.setFirstName("Gabriela");
        patient.setLastName("Pereira");
        patient.setBirthdate("1995-04-12");

        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setCanceled(false);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduleTime(slot);
        return appointment;
    }
}
