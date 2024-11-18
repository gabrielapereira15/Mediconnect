package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.DoctorDetails;

import java.util.List;

public interface DoctorClient {
    List<Doctor> getDoctors();

    DoctorDetails getDoctor(String doctorId);
}
