package com.example.mediconnect_android.client.response;

/** What /auth/verify-otp gives back on success. */
public class AuthResult {

    private String token;
    private String email;
    private long expiresInSeconds;
    private boolean newPatient;

    /** Set by the client when the request itself failed. */
    private transient boolean verified;

    public static AuthResult failed() {
        var result = new AuthResult();
        result.verified = false;
        return result;
    }

    public boolean isVerified() {
        return verified && token != null && !token.isEmpty();
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    /** True when this sign-in created the patient record, so the profile is empty. */
    public boolean isNewPatient() {
        return newPatient;
    }
}
