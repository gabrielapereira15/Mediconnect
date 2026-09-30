package com.vegs.mediconnect.backoffice.appointment;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which tab a booking sits under, and what its badge says (board B03).
 *
 * Both are read off the booking rather than stored, so these are where the
 * rules are written down.
 */
class AppointmentsViewTest {

    private static final LocalDateTime NOW = LocalDate.of(2026, 9, 29).atTime(12, 0);
    private final Doctor chase = doctor("Robert", "Chase");
    private final Doctor cameron = doctor("Allison", "Cameron");

    @Test
    @DisplayName("today, ahead, behind and cancelled each have their own tab")
    void tabs() {
        var today = NOW.toLocalDate();
        assertEquals(AppointmentsView.Tab.TODAY, AppointmentsView.tabOf(at(0, 9), today));
        assertEquals(AppointmentsView.Tab.UPCOMING, AppointmentsView.tabOf(at(1, 9), today));
        assertEquals(AppointmentsView.Tab.PAST, AppointmentsView.tabOf(at(-1, 9), today));
        var cancelled = at(1, 9);
        cancelled.setCanceled(true);
        assertEquals(AppointmentsView.Tab.CANCELLED, AppointmentsView.tabOf(cancelled, today));
    }

    @Test
    @DisplayName("a visit ahead says what is still missing: the form first, then attendance")
    void statusAhead() {
        var noForm = at(1, 9);
        assertEquals(AppointmentsView.Status.FORM_PENDING, AppointmentsView.statusOf(noForm, NOW));

        var formIn = at(1, 9);
        formIn.setFormSubmittedAt(OffsetDateTime.now());
        assertEquals(AppointmentsView.Status.BOOKED, AppointmentsView.statusOf(formIn, NOW));

        formIn.setAttendanceConfirmedAt(OffsetDateTime.now());
        assertEquals(AppointmentsView.Status.CONFIRMED, AppointmentsView.statusOf(formIn, NOW));
    }

    @Test
    @DisplayName("a visit behind us is completed only if someone saw them arrive")
    void statusBehind() {
        var arrived = at(0, 9);
        arrived.setCheckedInAt(OffsetDateTime.now());
        assertEquals(AppointmentsView.Status.COMPLETED, AppointmentsView.statusOf(arrived, NOW));

        var unknown = at(0, 9);
        assertEquals(AppointmentsView.Status.NOT_CHECKED_IN, AppointmentsView.statusOf(unknown, NOW),
                "not a no-show: the clinic may simply not use check-in");
    }

    @Test
    @DisplayName("a moved visit is listed once, at its new time")
    void movedVisitsAreNotListedTwice() {
        var moved = at(2, 9);
        moved.setStatus(AppointmentStatus.REMOVED.getStatus());
        moved.setCanceled(true);

        var page = AppointmentsView.build(List.of(moved, at(3, 9)), filters(AppointmentsView.Tab.CANCELLED), NOW);

        assertEquals(0, page.count(AppointmentsView.Tab.CANCELLED));
        assertEquals(1, page.count(AppointmentsView.Tab.UPCOMING));
    }

    @Test
    @DisplayName("filters narrow every tab's count, not only the list")
    void filtersNarrowCounts() {
        var withChase = at(1, 9);
        var withCameron = at(1, 10);
        withCameron.setDoctor(cameron);
        var pastWithChase = at(-2, 9);

        var filters = new AppointmentsView.Filters(AppointmentsView.Tab.UPCOMING, null, chase.getId(),
                AppointmentsView.Range.ANY, false, 0);
        var page = AppointmentsView.build(List.of(withChase, withCameron, pastWithChase), filters, NOW);

        assertEquals(1, page.count(AppointmentsView.Tab.UPCOMING));
        assertEquals(1, page.count(AppointmentsView.Tab.PAST));
        assertEquals(1, page.rows().size());
    }

    @Test
    @DisplayName("form pending means still ahead, not cancelled, and nothing sent")
    void formPendingFilter() {
        var pending = at(1, 9);
        var sent = at(1, 10);
        sent.setFormSubmittedAt(OffsetDateTime.now());
        var over = at(-1, 9);

        var filters = new AppointmentsView.Filters(AppointmentsView.Tab.UPCOMING, "", null,
                AppointmentsView.Range.ANY, true, 0);

        assertTrue(AppointmentsView.matches(pending, filters, NOW));
        assertFalse(AppointmentsView.matches(sent, filters, NOW));
        assertFalse(AppointmentsView.matches(over, filters, NOW), "chasing a form after the visit helps nobody");
    }

    @Test
    @DisplayName("search finds the patient, the person booked for, or the doctor")
    void search() {
        var forDaughter = at(1, 9);
        forDaughter.setBookedForName("Sofia Almeida");

        assertTrue(AppointmentsView.matches(forDaughter, query("sofia"), NOW));
        assertTrue(AppointmentsView.matches(forDaughter, query("PEREIRA"), NOW));
        assertTrue(AppointmentsView.matches(forDaughter, query("chase"), NOW));
        assertFalse(AppointmentsView.matches(forDaughter, query("wilson"), NOW));
    }

    @Test
    @DisplayName("ahead soonest first, pages of twenty, and a page past the end shows the last")
    void ordersAndPages() {
        List<Appointment> many = new ArrayList<>();
        for (int day = 25; day >= 1; day--) {
            many.add(at(day, 9));
        }

        var first = AppointmentsView.build(many, filters(AppointmentsView.Tab.UPCOMING), NOW);
        assertEquals(20, first.rows().size());
        assertEquals(2, first.pages());
        assertEquals("Wed 30 Sep", first.rows().getFirst().day());

        var beyond = AppointmentsView.build(many, new AppointmentsView.Filters(AppointmentsView.Tab.UPCOMING,
                null, null, AppointmentsView.Range.ANY, false, 9), NOW);
        assertEquals(1, beyond.page());
        assertEquals(5, beyond.rows().size());
        assertEquals(21, beyond.from());
    }

    // ---- fixtures ---------------------------------------------------------------

    private AppointmentsView.Filters filters(AppointmentsView.Tab tab) {
        return new AppointmentsView.Filters(tab, null, null, AppointmentsView.Range.ANY, false, 0);
    }

    private AppointmentsView.Filters query(String q) {
        return new AppointmentsView.Filters(AppointmentsView.Tab.UPCOMING, q, null,
                AppointmentsView.Range.ANY, false, 0);
    }

    private static Doctor doctor(String first, String last) {
        var doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setFirstName(first);
        doctor.setLastName(last);
        return doctor;
    }

    private Appointment at(int daysFromNow, int hour) {
        var schedule = new Schedule();
        schedule.setDate(NOW.toLocalDate().plusDays(daysFromNow));
        schedule.setDoctor(chase);

        var slot = new ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(LocalTime.of(hour, 0));
        slot.setSchedule(schedule);

        var patient = new Patient();
        patient.setFirstName("Gabriela");
        patient.setLastName("Pereira");
        patient.setBirthdate("1995-04-12");

        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setCanceled(false);
        appointment.setStatus(AppointmentStatus.UPCOMING.getStatus());
        appointment.setPatient(patient);
        appointment.setDoctor(chase);
        appointment.setScheduleTime(slot);
        return appointment;
    }
}
