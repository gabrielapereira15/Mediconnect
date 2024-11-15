package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;

import java.util.List;

public interface AppointmentClient {
    List<Doctor> getDoctors();

    Doctor getDoctor(String doctorId);

    List<Appointment> getAppointments(int patientId);

    List<Schedule> getSchedules(int doctorId);
}
