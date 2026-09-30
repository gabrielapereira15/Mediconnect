package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.BookingResult;
import com.example.mediconnect_android.model.Doctor;

import java.util.List;

public interface AppointmentClient {
    List<Doctor> getDoctors();

    List<Appointment> getAppointments(String email);

    BookingResult createAppointment(String appointmentJson);

    Boolean cancelAppointment(String appointmentId);

    /**
     * Tells the clinic the patient has arrived, and returns the appointment
     * as it now stands — null when the clinic would not take it, which is
     * any day but the day of the visit.
     */
    Appointment checkIn(String appointmentId);

    /** "I will be there". The updated appointment, or null if refused. */
    Appointment confirmAttendance(String appointmentId);
}
