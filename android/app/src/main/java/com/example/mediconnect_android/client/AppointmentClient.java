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
}
