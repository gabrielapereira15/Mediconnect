package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class WaitlistClientImpl implements WaitlistClient {

    private static final String TAG = "WaitlistClientImpl";

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
