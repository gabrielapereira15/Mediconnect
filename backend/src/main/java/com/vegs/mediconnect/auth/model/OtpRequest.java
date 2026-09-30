package com.vegs.mediconnect.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body of POST /auth/get-otp. */
@Getter
@Setter
public class OtpRequest {

    @NotBlank
    @Email
    private String email;

    /** Sent by the app; only "patient" is supported today. */
    private String role = "patient";
}
