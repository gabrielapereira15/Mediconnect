package com.example.mediconnect_android.client;

public interface OTPClient {

    boolean sendOTP(String email, String role);

    boolean verifyOTP(String email, String otp);

}
