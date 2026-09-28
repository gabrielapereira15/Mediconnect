package com.example.mediconnect_android.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Notification;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The demo catalogue is what an installed APK shows when no backend is
 * running, so it has to be complete and self-consistent — an empty or
 * half-populated list there looks like a broken app, not a demo.
 */
public class DemoDataTest {

    @Test
    public void doctorsAreFullyPopulated() {
        List<Doctor> doctors = DemoData.doctors();

        assertFalse("demo mode would show an empty shop", doctors.isEmpty());

        for (Doctor doctor : doctors) {
            assertNotNull(doctor.getId());
            assertNotNull(doctor.getFirstName());
            assertNotNull(doctor.getLastName());
            assertNotNull("the list binds getName(), not the parts", doctor.getName());
            assertNotNull(doctor.getSpecialty());
            assertNotNull("a null score renders as a blank rating", doctor.getScore());
        }
    }

    @Test
    public void doctorIdsAreUnique() {
        List<Doctor> doctors = DemoData.doctors();
        long distinct = doctors.stream().map(Doctor::getId).distinct().count();

        assertEquals("duplicate ids break list selection", doctors.size(), distinct);
    }

    @Test
    public void doctorLookupFallsBackRatherThanReturningNull() {
        assertNotNull(DemoData.doctorById("demo-3"));
        assertEquals("demo-3", DemoData.doctorById("demo-3").getId());
        // An unknown id must still render something.
        assertNotNull(DemoData.doctorById("does-not-exist"));
    }

    @Test
    public void appointmentsCoverEveryTab() {
        List<Appointment> appointments = DemoData.appointments();

        assertTrue(hasStatus(appointments, "UPCOMING"));
        assertTrue(hasStatus(appointments, "COMPLETED"));
        assertTrue(hasStatus(appointments, "CANCELED"));

        for (Appointment appointment : appointments) {
            assertNotNull(appointment.getId());
            assertNotNull(appointment.getDate());
            assertNotNull(appointment.getTime());
            assertNotNull("each row shows the doctor", appointment.getDoctor());
        }
    }

    @Test
    public void appointmentDatesAvoidWeekends() {
        // The clinic has no weekend schedules, so a demo appointment on a
        // Saturday would be something the real app could never produce.
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);

        for (Appointment appointment : DemoData.appointments()) {
            LocalDate date = LocalDate.parse(
                    appointment.getDate() + " " + LocalDate.now().getYear(),
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH));

            assertFalse("weekend appointment: " + appointment.getDate(),
                    date.getDayOfWeek() == DayOfWeek.SATURDAY
                            || date.getDayOfWeek() == DayOfWeek.SUNDAY);
            // And the label round-trips, so it is a date the UI can show.
            assertEquals(appointment.getDate(), date.format(format));
        }
    }

    @Test
    public void notificationsArePopulated() {
        List<Notification> notifications = DemoData.notifications();

        assertFalse(notifications.isEmpty());
        for (Notification notification : notifications) {
            assertNotNull(notification.getId());
            assertNotNull(notification.getTitle());
            assertNotNull(notification.getMessage());
        }
    }

    @Test
    public void specialtiesAreDerivedAndDeduplicated() {
        List<String> specialties = DemoData.specialties();

        assertFalse(specialties.isEmpty());
        assertEquals(specialties.size(), specialties.stream().distinct().count());
        for (String specialty : specialties) {
            assertTrue("every specialty must belong to a doctor",
                    DemoData.doctors().stream()
                            .anyMatch(d -> d.getSpecialty().equals(specialty)));
        }
    }

    private static boolean hasStatus(List<Appointment> appointments, String status) {
        return appointments.stream().anyMatch(a -> status.equals(a.getStatus()));
    }
}
