package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.client.response.AuthResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class OTPClientImpl implements OTPClient {

    private static final String TAG = "OTPClientImpl";
    private final Gson gson = new Gson();

    @Override
    public boolean sendOTP(String email, String role) {
        String url = ApiConfig.url("/auth/get-otp");
        String body = gson.toJson(new OtpRequestBody(email, role));
        ApiGenericResponse response = OkHttpClientHelper.post(url, body);
        return response.isSuccess();
    }

    @Override
    public AuthResult verifyOTP(String email, String otp) {
        String url = ApiConfig.url("/auth/verify-otp");
        String body = gson.toJson(new VerifyRequestBody(email, otp));
        ApiGenericResponse response = OkHttpClientHelper.post(url, body);

        if (!response.isSuccess()) {
            return AuthResult.failed();
        }

        try {
            AuthResult result = gson.fromJson(response.getResponseBody(), AuthResult.class);
            if (result == null) {
                return AuthResult.failed();
            }
            result.setVerified(true);
            return result;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Could not read the verify-otp response", e);
            return AuthResult.failed();
        }
    }

    /** Serialised by Gson, so the values are escaped properly. */
    private record OtpRequestBody(String email, String role) {
    }

    private record VerifyRequestBody(String email, String otp) {
    }
}
