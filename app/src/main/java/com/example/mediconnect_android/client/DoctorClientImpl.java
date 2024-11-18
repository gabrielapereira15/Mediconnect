package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.DoctorDetails;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

import okhttp3.OkHttpClient;

public class DoctorClientImpl implements DoctorClient {
    private final String baseurl = "https://mediconnect-latest.onrender.com";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public List<Doctor> getDoctors() {
        String url = baseurl + "/api/doctors";
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type doctorListType = new TypeToken<List<Doctor>>() {}.getType();
            Gson gson = new Gson();
            List<Doctor> doctors = gson.fromJson(response.getResponseBody(), doctorListType);
            return doctors;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public DoctorDetails getDoctor(String doctorId) {
        String url = baseurl + "/api/mobile/doctors/" + doctorId;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type doctorListType = new TypeToken<DoctorDetails>() {}.getType();
            Gson gson = new Gson();
            DoctorDetails doctorDetails = gson.fromJson(response.getResponseBody(), doctorListType);
            return doctorDetails;
        } else {
            return null;
        }
    }
}
