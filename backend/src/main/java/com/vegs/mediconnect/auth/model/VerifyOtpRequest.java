package com.vegs.mediconnect.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body of POST /auth/verify-otp. */
@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;
}
