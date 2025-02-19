package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class AppointmentClientImpl implements AppointmentClient {
    private final String baseurl = "https://mediconnect-latest.onrender.com";

    @Override
    public List<Doctor> getDoctors() {
        return Collections.emptyList();
    }

    @Override
    public List<Appointment> getAppointments(String email) {
        String url = baseurl + "/api/mobile/appointments/" + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            Type appointmentListType = new TypeToken<List<Appointment>>() {
            }.getType();
            Gson gson = new Gson();
            return gson.fromJson(response.getResponseBody(), appointmentListType);
        } else {
            Log.e("AppointmentClientImpl", "Error getting appointments: " + response.getResponseBody());
            return Collections.emptyList();
        }
    }

    @Override
    public Boolean createAppointment(String appointmentJson) {
        String url = baseurl + "/api/mobile/appointments";
        ApiGenericResponse response = OkHttpClientHelper.post(url, appointmentJson);
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("AppointmentClientImpl", "Error creating appointment: " + response.getResponseBody());
            return false;
        }
    }

    @Override
    public Boolean cancelAppointment(String appointmentId) {
        String url = baseurl + "/api/mobile/appointments/cancel/" + appointmentId;
        ApiGenericResponse response = OkHttpClientHelper.put(url);
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("AppointmentClientImpl", "Error canceling appointment: " + response.getResponseBody());
            return false;
        }
    }
}
