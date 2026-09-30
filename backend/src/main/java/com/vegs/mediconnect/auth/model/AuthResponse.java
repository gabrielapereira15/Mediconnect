package com.vegs.mediconnect.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Reply to a successful POST /auth/verify-otp. */
@Getter
@AllArgsConstructor
public class AuthResponse {

    /** Bearer token for the Authorization header on later requests. */
    private String token;

    private String email;

    private long expiresInSeconds;

    /** True when this sign-in created the patient record. */
    private boolean newPatient;
}
