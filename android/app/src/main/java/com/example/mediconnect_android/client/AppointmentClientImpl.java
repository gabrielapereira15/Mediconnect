package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.data.DemoData;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.BookingResult;
import com.example.mediconnect_android.model.Doctor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class AppointmentClientImpl implements AppointmentClient {

    @Override
    public List<Doctor> getDoctors() {
        return Collections.emptyList();
    }

    @Override
    public List<Appointment> getAppointments(String email) {
        String url = ApiConfig.url("/api/mobile/appointments/") + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            Type appointmentListType = new TypeToken<List<Appointment>>() {
            }.getType();
            Gson gson = new Gson();
            DemoMode.disable();
            return gson.fromJson(response.getResponseBody(), appointmentListType);
        } else {
            Log.e("AppointmentClientImpl", "Error getting appointments: " + response.getResponseBody());
            if (OkHttpClientHelper.isOffline(response)) {
                DemoMode.enable();
                return DemoData.appointments();
            }
            throw new ApiException(response.getStatus(), "Could not load appointments");
        }
    }

    @Override
    public BookingResult createAppointment(String appointmentJson) {
        String url = ApiConfig.url("/api/mobile/appointments");
        ApiGenericResponse response = OkHttpClientHelper.post(url, appointmentJson);
        if (response.isSuccess()) {
            return BookingResult.booked();
        }
        Log.e("AppointmentClientImpl", "Error creating appointment: " + response.getResponseBody());
        // 409 means the slot went between the patient seeing it and
        // confirming, which is worth saying in those words.
        if (response.getStatus() == 409) {
            return BookingResult.slotTaken(response.getResponseBody());
        }
        return BookingResult.failed();
    }

    @Override
    public Boolean cancelAppointment(String appointmentId) {
        String url = ApiConfig.url("/api/mobile/appointments/cancel/") + appointmentId;
        ApiGenericResponse response = OkHttpClientHelper.put(url);
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("AppointmentClientImpl", "Error canceling appointment: " + response.getResponseBody());
            return false;
        }
    }
}
