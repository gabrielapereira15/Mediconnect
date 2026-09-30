package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each cell of the schedule says (board B02).
 */
class ScheduleBoardTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 29);
    private static final LocalDateTime NOON = DAY.atTime(12, 0);

    private final Doctor chase = doctor("Robert", "Chase", "Surgeon");
    private final Doctor cameron = doctor("Allison", "Cameron", "Cardiologist");

    @Test
    @DisplayName("a column per doctor, in name order, with how full their day is")
    void columns() {
        var a = slot(chase, 9, 0);
        var b = slot(chase, 9, 30);
        var c = slot(cameron, 9, 0);
        var booked = appointment(b);

        var day = ScheduleBoard.build(DAY, List.of(a, b, c), List.of(booked), Map.of(), null, NOON);

        assertEquals(List.of("Dr. Cameron", "Dr. Chase"),
                day.columns().stream().map(ScheduleBoard.Column::name).toList());
        assertEquals(1, day.columns().get(1).booked());
        assertEquals(2, day.columns().get(1).total());
    }

    @Test
    @DisplayName("a doctor with no slot at a time gets an empty cell, not someone else's")
    void emptyCells() {
        var day = ScheduleBoard.build(DAY, List.of(slot(chase, 9, 0), slot(cameron, 9, 30)),
                List.of(), Map.of(), null, NOON);

        assertNull(day.rows().get(0).cells().get(0), "Cameron has nothing at 9:00");
        assertNull(day.rows().get(1).cells().get(1), "Chase has nothing at 9:30");
    }

    @Test
    @DisplayName("a break longer than the usual step is its own row")
    void lunchIsAGap() {
        var slots = List.of(slot(chase, 11, 0), slot(chase, 11, 30), slot(chase, 13, 0), slot(chase, 13, 30));

        var day = ScheduleBoard.build(DAY, slots, List.of(), Map.of(), null, NOON);

        assertEquals(5, day.rows().size());
        assertTrue(day.rows().get(2).isGap());
        assertEquals("12:00–13:00", day.rows().get(2).gapLabel());
    }

    @Test
    @DisplayName("each state says what it is in words")
    void states() {
        var free = slot(chase, 15, 0);
        assertEquals("Free", cell(free, null, false, null).title());

        var freed = slot(chase, 15, 0);
        assertEquals(ScheduleBoard.State.FREED, cell(freed, null, true, null).state());

        var blocked = slot(chase, 15, 0);
        blocked.setBlocked(true);
        blocked.setAvailable(false);
        assertEquals("Blocked", cell(blocked, null, false, null).title());

        var held = slot(chase, 15, 0);
        held.setAvailable(false);
        var hold = new ScheduleBoard.Hold("Gabriela Pereira", DAY.atTime(14, 5));
        var heldCell = cell(held, null, false, hold);
        assertEquals(ScheduleBoard.State.HELD, heldCell.state());
        assertEquals("Offered to Gabriela · until 2:05 PM", heldCell.detail());

        var booked = slot(chase, 15, 0);
        var visit = appointment(booked);
        assertEquals("Booked · form pending", cell(booked, visit, false, null).detail());
        visit.setFormSubmittedAt(OffsetDateTime.now());
        visit.setAttendanceConfirmedAt(OffsetDateTime.now());
        assertEquals("Confirmed", cell(booked, visit, false, null).detail());
    }

    @Test
    @DisplayName("behind us, a slot is completed, not checked in, or simply unused")
    void past() {
        var earlier = slot(chase, 9, 0);
        var visit = appointment(earlier);
        assertEquals("Not checked in", cell(earlier, visit, false, null).detail());

        visit.setCheckedInAt(OffsetDateTime.now());
        assertEquals("Completed", cell(earlier, visit, false, null).detail());

        assertEquals("Unused", cell(slot(chase, 9, 30), null, false, null).title());
    }

    @Test
    @DisplayName("the booked-for name is the one on the schedule")
    void bookedForName() {
        var s = slot(chase, 15, 0);
        var visit = appointment(s);
        visit.setBookedForName("Sofia Almeida");

        assertEquals("Sofia Almeida", cell(s, visit, false, null).title());
    }

    @Test
    @DisplayName("a moved or cancelled visit does not count as booked")
    void inactiveVisits() {
        var s = slot(chase, 15, 0);
        var moved = appointment(s);
        moved.setStatus(AppointmentStatus.REMOVED.getStatus());
        moved.setCanceled(true);

        var day = ScheduleBoard.build(DAY, List.of(s), List.of(moved), Map.of(), null, NOON);

        assertEquals(0, day.columns().getFirst().booked());
    }

    @Test
    @DisplayName("a specialty filter keeps only those doctors")
    void specialtyFilter() {
        var day = ScheduleBoard.build(DAY, List.of(slot(chase, 9, 0), slot(cameron, 9, 0)),
                List.of(), Map.of(), "surgeon", NOON);

        assertEquals(1, day.columns().size());
        assertEquals("Dr. Chase", day.columns().getFirst().name());
    }

    // ---- fixtures -------------------------------------------------------------------

    private ScheduleBoard.Cell cell(ScheduleTime slot, Appointment appointment, boolean cancelled,
                                    ScheduleBoard.Hold hold) {
        return ScheduleBoard.cellFor(slot, appointment, cancelled, hold, NOON);
    }

    private static Doctor doctor(String first, String last, String specialty) {
        var doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setFirstName(first);
        doctor.setLastName(last);
        doctor.setSpecialty(specialty);
        return doctor;
    }

    private static ScheduleTime slot(Doctor doctor, int hour, int minute) {
        var schedule = new Schedule();
        schedule.setDate(DAY);
        schedule.setDoctor(doctor);
        var slot = new ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(LocalTime.of(hour, minute));
        slot.setSchedule(schedule);
        slot.setAvailable(true);
        slot.setBlocked(false);
        return slot;
    }

    private static Appointment appointment(ScheduleTime slot) {
        slot.setAvailable(false);
        var patient = new Patient();
        patient.setFirstName("Gabriela");
        patient.setLastName("Pereira");
        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setCanceled(false);
        appointment.setStatus(AppointmentStatus.UPCOMING.getStatus());
        appointment.setPatient(patient);
        appointment.setDoctor(slot.getSchedule().getDoctor());
        appointment.setScheduleTime(slot);
        return appointment;
    }
}
