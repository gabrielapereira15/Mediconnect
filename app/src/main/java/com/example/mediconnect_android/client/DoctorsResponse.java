package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Doctor;

import java.util.List;

public class DoctorsResponse {
    private List<Doctor> doctors;

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctors = doctors;
    }
}