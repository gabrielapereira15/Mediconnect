package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.Patient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class PatientClientImpl implements PatientClient {

    @Override
    public Patient getPatient(String patientEmail) {
        String url = ApiConfig.url("/api/mobile/patients/") + patientEmail;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type patientType = new TypeToken<Patient>() {
            }.getType();
            Gson gson = new Gson();
            Patient patient = gson.fromJson(response.getResponseBody(), patientType);
            return patient;
        } else {
            return null;
        }
    }

    @Override
    public List<Patient> getPatients() {
        return Collections.emptyList();
    }

    @Override
    public Boolean createPatient(String patient) {
        String url = ApiConfig.url("/api/mobile/patients");
        ApiGenericResponse response = OkHttpClientHelper.post(url, patient);
        return response.isSuccess();
    }
}
