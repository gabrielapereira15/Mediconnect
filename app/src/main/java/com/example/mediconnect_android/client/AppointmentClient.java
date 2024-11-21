package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;

import java.util.List;

public interface AppointmentClient {
    List<Doctor> getDoctors();

    List<Appointment> getAppointments(String email);

    Boolean createAppointment(String appointmentJson);
}
