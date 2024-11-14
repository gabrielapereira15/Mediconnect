package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Doctor;

import java.util.List;

public interface AppointmentClient {
    List<Doctor> getDoctors();

    Doctor getDoctor(String doctorId);
}
