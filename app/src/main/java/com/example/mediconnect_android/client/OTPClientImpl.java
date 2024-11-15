package com.example.mediconnect_android.client;

public class OTPClientImpl implements OTPClient {

    private final String baseurl = "https://mediconnect-backend-h6cg.onrender.com";

    @Override
    public void sendOTP(String email, String role) {
    }

    @Override
    public boolean verifyOTP(String email, String otp) {
        return true;
    }
}
