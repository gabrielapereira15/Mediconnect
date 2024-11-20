package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.Patient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class PatientClientImpl implements PatientClient {
    private final String baseurl = "https://mediconnect-latest.onrender.com";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public Patient getPatient(String patientEmail) {
        String url = baseurl + "/api/mobile/patients/email/" + patientEmail;
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
        String url = baseurl + "/api/mobile/patients";
        ApiGenericResponse response = OkHttpClientHelper.post(url, patient);
        if (response.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }
}
