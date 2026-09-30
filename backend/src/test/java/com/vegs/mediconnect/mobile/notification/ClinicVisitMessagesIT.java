package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.backoffice.appointment.ClinicAppointmentService;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.appointment.AppointmentApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whatever the clinic does to a visit from the back office, the patient
 * is told in the app. Moving a visit used to change it silently.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class ClinicVisitMessagesIT {

    private static final String EMAIL = "demo@mediconnect.ca";

    @Autowired
    private AppointmentApiService appointments;

    @Autowired
    private ClinicAppointmentService clinic;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationPatientRepository deliveries;

    private Patient patient() {
        return patients.findByEmail(EMAIL).orElseThrow();
    }

    private Appointment upcoming() {
        return appointmentRepository.findAllByPatient(patient()).stream()
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getDateTime().isAfter(LocalDateTime.now()))
                .min(Comparator.comparing(Appointment::getDateTime))
                .orElseThrow(() -> new AssertionError("the demo seed gives the demo patient a visit ahead"));
    }

    private UUID freeTimeWithSameDoctor(Appointment appointment) {
        return clinic.freeSlotsFor(appointment.getId()).values().stream()
                .flatMap(List::stream)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the demo doctor has free times ahead"))
                .id();
    }

    private List<NotificationPatient> messagesAbout(UUID appointmentId) {
        return deliveries.findAllByPatientId(patient()).stream()
                .filter(delivery -> appointmentId.equals(delivery.getNotificationId().getAppointmentId()))
                .toList();
    }

    /**
     * The messages about a visit that were not there before. The demo seed
     * sends its own reminder about the patient's next visit, and which visit
     * that is depends on the time of day, so counting every message about
     * the visit made these tests pass or fail with the clock.
     */
    private List<NotificationPatient> newMessagesAbout(UUID appointmentId, List<NotificationPatient> before) {
        var seen = before.stream().map(NotificationPatient::getId).toList();
        return messagesAbout(appointmentId).stream()
                .filter(delivery -> !seen.contains(delivery.getId()))
                .toList();
    }

    @Test
    @DisplayName("moving a visit tells the patient the old time and the new")
    void moved() {
        Appointment visit = upcoming();
        LocalDateTime before = visit.getDateTime();

        Appointment moved = appointments.rescheduleAsClinic(visit.getId(), freeTimeWithSameDoctor(visit));

        List<NotificationPatient> messages = messagesAbout(moved.getId());
        assertEquals(1, messages.size(), "one message, about the visit as it is now");
        var message = messages.get(0);
        assertFalse(Boolean.TRUE.equals(message.getAcknowledged()), "arrives unread, so the bell lights up");
        assertEquals("Your visit has moved", message.getNotificationId().getTitle());
        String body = message.getNotificationId().getMessage();
        assertTrue(body.contains(Display.dayAndTime(before)), body);
        assertTrue(body.contains(Display.dayAndTime(moved.getDateTime())), body);
    }

    @Test
    @DisplayName("cancelling a visit tells the patient, without the desk's reason")
    void cancelled() {
        Appointment visit = upcoming();
        List<NotificationPatient> before = messagesAbout(visit.getId());

        appointments.cancelAppointmentAsClinic(visit.getId(), "Doctor double-booked, internal");

        List<NotificationPatient> messages = newMessagesAbout(visit.getId(), before);
        assertEquals(1, messages.size());
        assertEquals("Your visit was cancelled", messages.get(0).getNotificationId().getTitle());
        String body = messages.get(0).getNotificationId().getMessage();
        assertTrue(body.contains(Display.dayAndTime(visit.getDateTime())), body);
        assertFalse(body.contains("double-booked"), "the reason is for the clinic, not the patient");
    }

    @Test
    @DisplayName("booking from the desk tells the patient when")
    void booked() {
        UUID slot = freeTimeWithSameDoctor(upcoming());

        Appointment booked = appointments.bookAsClinic(patient().getId(), slot, null);

        List<NotificationPatient> messages = messagesAbout(booked.getId());
        assertEquals(1, messages.size());
        assertEquals("You are booked in", messages.get(0).getNotificationId().getTitle());
        assertTrue(messages.get(0).getNotificationId().getMessage()
                .contains(Display.dayAndTime(booked.getDateTime())));
    }
}
