package com.vegs.mediconnect.auth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Reply to POST /auth/get-otp. */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {

    private String message;

    /**
     * The passcode itself, present only while mediconnect.auth.expose-otp is
     * on. There is no mail provider in the demo, so without this there would
     * be no way to sign in. Always null in production.
     */
    private String otp;

    private int expiresInMinutes;
}
