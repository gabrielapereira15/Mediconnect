package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.WaitlistEntry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class WaitlistClientImpl implements WaitlistClient {

    private static final String TAG = "WaitlistClientImpl";

    @Override
    public List<WaitlistEntry> list(String email) {
        String url = ApiConfig.url("/api/mobile/waitlist/") + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            Type listType = new TypeToken<List<WaitlistEntry>>() {
            }.getType();
            return new Gson().fromJson(response.getResponseBody(), listType);
        }
        Log.e(TAG, "Error listing waitlists: " + response.getResponseBody());
        return Collections.emptyList();
    }

    @Override
    public boolean leave(String email, String entryId) {
        String url = ApiConfig.url("/api/mobile/waitlist/") + email + "/" + entryId + "/leave";
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        }
        Log.e(TAG, "Error leaving the waitlist: " + response.getResponseBody());
        return false;
    }

    @Override
    public com.example.mediconnect_android.model.Appointment accept(String email, String entryId) {
        String url = ApiConfig.url("/api/mobile/waitlist/") + email + "/" + entryId + "/accept";
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return new com.google.gson.Gson().fromJson(response.getResponseBody(),
                    com.example.mediconnect_android.model.Appointment.class);
        }
        Log.e(TAG, "Error taking an offer: " + response.getResponseBody());
        throw new com.example.mediconnect_android.client.ApiException(
                response.getStatus(), response.getResponseBody());
    }

    @Override
    public boolean decline(String email, String entryId) {
        String url = ApiConfig.url("/api/mobile/waitlist/") + email + "/" + entryId + "/decline";
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        }
        Log.e(TAG, "Error turning an offer down: " + response.getResponseBody());
        return false;
    }

    @Override
    public boolean join(String email, String doctorId, String currentAppointmentDate) {
        String url = ApiConfig.url("/api/mobile/waitlist/") + email;

        JSONObject payload = new JSONObject();
        try {
            payload.put("doctorId", doctorId);
            payload.put("currentAppointmentDate", currentAppointmentDate);
        } catch (JSONException e) {
            Log.e(TAG, "Could not build the waitlist payload", e);
            return false;
        }

        ApiGenericResponse response = OkHttpClientHelper.post(url, payload.toString());
        if (response.isSuccess()) {
            return true;
        }

        Log.e(TAG, "Error joining the waitlist: " + response.getResponseBody());
        // 409 means they are already on it, which is not a failure worth
        // showing as an error.
        throw new ApiException(response.getStatus(), response.getStatus() == 409
                ? "You are already on the waitlist for this doctor."
                : "Could not join the waitlist");
    }
}
