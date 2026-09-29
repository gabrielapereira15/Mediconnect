package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.data.DemoData;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.model.HealthEntry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

public class HealthClientImpl implements HealthClient {

    private static final String TAG = "HealthClientImpl";

    @Override
    public List<HealthEntry> getEntries(String email) {
        String url = ApiConfig.url("/api/mobile/health/") + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);

        if (response.isSuccess()) {
            Type listType = new TypeToken<List<HealthEntry>>() {
            }.getType();
            DemoMode.disable();
            return new Gson().fromJson(response.getResponseBody(), listType);
        }

        Log.e(TAG, "Error loading the health record: " + response.getResponseBody());
        if (OkHttpClientHelper.isOffline(response)) {
            DemoMode.enable();
            return DemoData.healthEntries();
        }
        throw new ApiException(response.getStatus(), "Could not load your health information");
    }

    @Override
    public HealthEntry addEntry(String email, String type, String description,
                                String note, String onsetDate) {
        String url = ApiConfig.url("/api/mobile/health/") + email;

        // Built rather than formatted: a medication name can contain a quote
        // or a backslash, and either would produce a body the server cannot
        // read.
        JSONObject payload = new JSONObject();
        try {
            payload.put("type", type);
            payload.put("description", description);
            if (note != null && !note.isEmpty()) {
                payload.put("note", note);
            }
            if (onsetDate != null && !onsetDate.isEmpty()) {
                payload.put("onsetDate", onsetDate);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Could not build the health entry payload", e);
            throw new ApiException(0, "Could not save that entry");
        }

        ApiGenericResponse response = OkHttpClientHelper.post(url, payload.toString());
        if (response.isSuccess()) {
            return new Gson().fromJson(response.getResponseBody(), HealthEntry.class);
        }

        Log.e(TAG, "Error adding a health entry: " + response.getResponseBody());
        throw new ApiException(response.getStatus(), "Could not save that entry");
    }

    @Override
    public boolean stopEntry(String email, String entryId) {
        String url = ApiConfig.url("/api/mobile/health/") + email + "/" + entryId + "/stop";
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");

        if (response.isSuccess()) {
            return true;
        }
        Log.e(TAG, "Error stopping a health entry: " + response.getResponseBody());
        return false;
    }
}
