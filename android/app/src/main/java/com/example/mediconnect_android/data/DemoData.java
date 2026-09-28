package com.example.mediconnect_android.data;

import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Notification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sample clinic used when the API cannot be reached.
 *
 * It mirrors what the backend seeds, so the app is fully browsable from an
 * installed APK with no server running — which is how anyone trying the app
 * from a release download will see it. {@link DemoMode} records that the data
 * is sample data so the UI can say so rather than pretending it is live.
 */
public final class DemoData {

    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);

    private DemoData() {
    }

    public static List<Doctor> doctors() {
        List<Doctor> doctors = new ArrayList<>();
        doctors.add(doctor("demo-1", "Robert", "Chase", "Surgeon", 4.8, 24));
        doctors.add(doctor("demo-2", "Lisa", "Cuddy", "Optometrist", 4.9, 41));
        doctors.add(doctor("demo-3", "Allison", "Cameron", "Cardiologist", 4.7, 18));
        doctors.add(doctor("demo-4", "Eric", "Foreman", "Neurologist", 4.6, 12));
        doctors.add(doctor("demo-5", "Remy", "Hadley", "Gynecologist", 4.8, 31));
        doctors.add(doctor("demo-6", "James", "Wilson", "Pediatrician", 5.0, 57));
        doctors.add(doctor("demo-7", "Chris", "Taub", "Dermatologist", 4.5, 9));
        doctors.add(doctor("demo-8", "Amber", "Volakis", "General Practitioner", 4.7, 22));
        return doctors;
    }

    public static Doctor doctorById(String id) {
        return doctors().stream()
                .filter(doctor -> doctor.getId().equals(id))
                .findFirst()
                .orElse(doctors().get(0));
    }

    public static List<Appointment> appointments() {
        List<Doctor> doctors = doctors();
        List<Appointment> appointments = new ArrayList<>();

        appointments.add(appointment("demo-a1", "UPCOMING", businessDays(2), "10 a.m.", doctors.get(0)));
        appointments.add(appointment("demo-a2", "UPCOMING", businessDays(5), "2 p.m.", doctors.get(2)));
        appointments.add(appointment("demo-a3", "COMPLETED", businessDays(-6), "9:30 a.m.", doctors.get(1)));
        appointments.add(appointment("demo-a4", "COMPLETED", businessDays(-13), "11 a.m.", doctors.get(0)));
        appointments.add(appointment("demo-a5", "CANCELED", businessDays(3), "11 a.m.", doctors.get(5)));

        return appointments;
    }

    public static List<Notification> notifications() {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notification("demo-n1", "Appointment reminder",
                "You have an appointment with Dr. Chase in two days. "
                        + "Please complete your pre-appointment form beforehand."));
        notifications.add(notification("demo-n2", "Flu shots available",
                "Walk-in flu vaccinations are available every weekday between 9am and 4pm."));
        notifications.add(notification("demo-n3", "Clinic hours over the holidays",
                "The clinic closes at 1pm on December 24th and reopens on December 27th."));
        return notifications;
    }

    public static List<String> specialties() {
        List<String> specialties = new ArrayList<>();
        for (Doctor doctor : doctors()) {
            if (!specialties.contains(doctor.getSpecialty())) {
                specialties.add(doctor.getSpecialty());
            }
        }
        Collections.sort(specialties);
        return specialties;
    }

    private static Doctor doctor(String id, String firstName, String lastName,
                                 String specialty, double score, int reviewCount) {
        var doctor = new Doctor();
        doctor.setId(id);
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setName(lastName + ", " + firstName);
        doctor.setSpecialty(specialty);
        doctor.setScore(score);
        doctor.setReviewCount(reviewCount);
        // No photo: the adapters already fall back to a placeholder avatar.
        doctor.setPhoto(null);
        return doctor;
    }

    private static Appointment appointment(String id, String status, String date,
                                           String time, Doctor doctor) {
        var appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(status);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDoctor(doctor);
        appointment.setReviewed(false);
        return appointment;
    }

    private static Notification notification(String id, String title, String message) {
        var notification = new Notification();
        notification.setId(id);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreationDate(LocalDate.now().toString());
        return notification;
    }

    /** Formats a date the given number of clinic days away, skipping weekends. */
    private static String businessDays(int days) {
        LocalDate date = LocalDate.now();
        int step = days < 0 ? -1 : 1;
        for (int moved = 0; moved < Math.abs(days); ) {
            date = date.plusDays(step);
            if (date.getDayOfWeek().getValue() <= 5) {
                moved++;
            }
        }
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(step);
        }
        return date.format(DATE_LABEL);
    }
}
