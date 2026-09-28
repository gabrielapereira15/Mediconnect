package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.data.DemoData;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.DoctorDetails;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class DoctorClientImpl implements DoctorClient {

    @Override
    public List<Doctor> getDoctors() {
        String url = ApiConfig.url("/api/mobile/doctors");
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type doctorListType = new TypeToken<List<Doctor>>() {
            }.getType();
            Gson gson = new Gson();
            List<Doctor> doctors = gson.fromJson(response.getResponseBody(), doctorListType);
            DemoMode.disable();
            return doctors;
        }

        // No server at all: show the bundled clinic rather than an empty screen.
        if (OkHttpClientHelper.isOffline(response)) {
            DemoMode.enable();
            return DemoData.doctors();
        }

        // The server answered and refused. Surfacing that as an empty list
        // would make a failure look like an empty clinic.
        Log.e("DoctorClientImpl", "Error getting doctors: " + response.getResponseBody());
        throw new ApiException(response.getStatus(), "Could not load doctors");
    }

    @Override
    public DoctorDetails getDoctor(String doctorId) {
        String url = ApiConfig.url("/api/mobile/doctors/") + doctorId;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type doctorListType = new TypeToken<DoctorDetails>() {
            }.getType();
            Gson gson = new Gson();
            DoctorDetails doctorDetails = gson.fromJson(response.getResponseBody(), doctorListType);
            return doctorDetails;
        } else {
            Log.e("DoctorClientImpl", "Error getting doctor details: " + response.getResponseBody());
            return null;
        }
    }
}
