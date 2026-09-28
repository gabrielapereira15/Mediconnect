package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.AuthResult;

public interface OTPClient {

    /** Asks the server to send a passcode to this email. */
    boolean sendOTP(String email, String role);

    /**
     * Exchanges a passcode for a bearer token. Check
     * {@link AuthResult#isVerified()} before using the result.
     */
    AuthResult verifyOTP(String email, String otp);
}
