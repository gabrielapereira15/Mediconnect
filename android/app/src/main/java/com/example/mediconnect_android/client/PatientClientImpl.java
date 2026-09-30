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

    /**
     * Saves the profile. A 400 with a plain-text reason is thrown as an
     * {@link ApiException} carrying it: the server words those for the
     * patient, for instance a health card number no province issues, and
     * "try again later" would send them to retry something that cannot work.
     * Anything else unsuccessful stays a plain false.
     */
    @Override
    public Boolean createPatient(String patient) {
        String url = ApiConfig.url("/api/mobile/patients");
        ApiGenericResponse response = OkHttpClientHelper.post(url, patient);
        if (response.getStatus() == 400) {
            String reason = response.getResponseBody() == null ? "" : response.getResponseBody().trim();
            // A JSON body is the framework's own validation report, which is
            // written for developers rather than patients.
            if (!reason.isEmpty() && !reason.startsWith("{")) {
                throw new ApiException(400, reason);
            }
        }
        return response.isSuccess();
    }
}
