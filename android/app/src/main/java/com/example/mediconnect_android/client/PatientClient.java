package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Patient;

import java.util.List;

public interface PatientClient {
    Patient getPatient(String patientEmail);

    List<Patient> getPatients();

    /**
     * Creates or updates the patient with this JSON body.
     *
     * @throws ApiException with status 400 when the server refused a field
     *         and said why, in words meant for the patient
     */
    Boolean createPatient(String patient);
}
