package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.ApiGenericResponse;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OTPClientImpl implements OTPClient {

    private final String baseurl = "https://mediconnect-backend-h6cg.onrender.com";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public void sendOTP(String email, String role) {
        String url = baseurl + "/auth/get-otp";
        String jsonData = String.format("{\"email\": \"%s\", \"role\": \"%s\"}", email, role);
        ApiGenericResponse response = OkHttpClientHelper.post(url, jsonData);

    }

    @Override
    public boolean verifyOTP(String email, String otp) {
        // Implement this logic similarly if needed
        return true;
    }
}
