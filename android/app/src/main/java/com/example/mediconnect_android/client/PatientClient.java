package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Patient;

import java.util.List;

public interface PatientClient {
    Patient getPatient(String patientEmail);

    List<Patient> getPatients();

    Boolean createPatient(String patient);
}
