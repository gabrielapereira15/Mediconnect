package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Messages the clinic sends one patient about one of their visits.
 *
 * Whatever the clinic does to a visit — books it, moves it, cancels it —
 * the patient hears about from the clinic, rather than finding out by
 * turning up at the old time. Each message lands in the patient's
 * Messages carrying the visit, so the app can offer the next thing to do
 * with it (see it, or fill in its form).
 */
@Service
@RequiredArgsConstructor
public class PatientMessages {

    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;

    /** The front desk booked the patient in (board B03's New appointment). */
    public void visitBooked(Appointment appointment) {
        send(appointment, "You are booked in",
                "The clinic booked " + whose(appointment) + " with " + doctor(appointment)
                        + " on " + Display.dayAndTime(appointment.getDateTime()) + ".");
    }

    /** The front desk moved the visit to another time with the same doctor. */
    public void visitMoved(LocalDateTime from, Appointment to) {
        send(to, "Your visit has moved",
                "The clinic moved " + whose(to) + " with " + doctor(to)
                        + " from " + Display.dayAndTime(from)
                        + " to " + Display.dayAndTime(to.getDateTime()) + "."
                        + " If the new time does not suit you, you can cancel it in Visits and book another.");
    }

    /**
     * The clinic cancelled the visit. The reason the desk wrote down is
     * for the clinic — it can say anything — so it stays on the
     * appointment and out of the message.
     */
    public void visitCancelled(Appointment appointment) {
        send(appointment, "Your visit was cancelled",
                "The clinic cancelled " + whose(appointment) + " with " + doctor(appointment)
                        + " on " + Display.dayAndTime(appointment.getDateTime()) + "."
                        + " You can book another time in the app whenever it suits you.");
    }

    /** A nudge about a form still to fill in, sent from the back office. */
    public void formReminder(Appointment appointment) {
        send(appointment, "Please fill in your pre-appointment form",
                doctor(appointment) + " would like your answers before your visit on "
                        + Display.dayAndTime(appointment.getDateTime())
                        + ". It takes about three minutes.");
    }

    private void send(Appointment appointment, String title, String message) {
        var notification = new Notification();
        notification.setKind(NotificationKind.APPOINTMENT);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setAppointmentId(appointment.getId());
        notification.setSendAllPatients(false);
        notification.setIsDeleted(false);
        var saved = notificationRepository.save(notification);

        var delivery = new NotificationPatient();
        delivery.setNotificationId(saved);
        delivery.setPatientId(appointment.getPatient());
        delivery.setAcknowledged(false);
        notificationPatientRepository.save(delivery);
    }

    private static String doctor(Appointment appointment) {
        return "Dr. " + appointment.getDoctor().getLastName();
    }

    /** "your visit", or "Leo's visit" when it was booked for someone else. */
    private static String whose(Appointment appointment) {
        String name = appointment.getBookedForName();
        return name == null || name.isBlank() ? "your visit" : name.trim() + "'s visit";
    }
}
