package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.PreVisitForm;
import com.google.gson.Gson;

public class PreVisitFormClientImpl implements PreVisitFormClient {

    private static final String TAG = "PreVisitFormClient";

    @Override
    public PreVisitForm submit(String appointmentId, PreVisitForm form) {
        String url = ApiConfig.url("/api/mobile/appointments/") + appointmentId + "/form";
        ApiGenericResponse response =
                OkHttpClientHelper.put(url, new Gson().toJson(form));
        if (response.isSuccess()) {
            return new Gson().fromJson(response.getResponseBody(), PreVisitForm.class);
        }
        Log.e(TAG, "Could not send the form: " + response.getResponseBody());
        return null;
    }

    @Override
    public PreVisitForm get(String appointmentId) {
        String url = ApiConfig.url("/api/mobile/appointments/") + appointmentId + "/form";
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        // 204 means the form has not been sent; the body is empty, not an error.
        if (response.isSuccess() && response.getResponseBody() != null
                && !response.getResponseBody().isBlank()) {
            return new Gson().fromJson(response.getResponseBody(), PreVisitForm.class);
        }
        return null;
    }
}
