package com.vegs.mediconnect.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Body of POST /auth/get-otp. */
@Getter
@Setter
public class OtpRequest {

    /**
     * Plain ASCII only. Java's case-insensitive compare, which the ownership
     * checks use, treats the dotless and dotted Turkish i and the long s as
     * i and s, while lower-casing keeps them apart. So a lookalike address
     * got its own passcode budget and then a token every check accepted for
     * the real patient.
     */
    @Pattern(regexp = "\\s*[\\x21-\\x7E]+\\s*",
            message = "Use an email address written in plain letters, digits and symbols.")
    @NotBlank
    @Email
    private String email;

    /** Sent by the app; only "patient" is supported today. */
    private String role = "patient";
}
