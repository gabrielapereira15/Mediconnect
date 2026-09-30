package com.vegs.mediconnect.auth;

import com.vegs.mediconnect.auth.model.AuthResponse;
import com.vegs.mediconnect.auth.model.OtpRequest;
import com.vegs.mediconnect.auth.model.OtpResponse;
import com.vegs.mediconnect.auth.model.VerifyOtpRequest;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Passwordless sign-in.
 *
 * The client asks for a passcode, then exchanges it for a bearer token. These
 * are the two endpoints the Android app has always called; they previously
 * lived on a separate service that no longer exists.
 */
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String DEFAULT_CLINIC_CODE = "KIT001";

    private final OtpService otpService;
    private final TokenService tokenService;
    private final AuthProperties properties;
    private final PatientRepository patientRepository;

    /** POST /auth/get-otp — creates a passcode for an email address. */
    @PostMapping("/get-otp")
    public ResponseEntity<OtpResponse> getOtp(@RequestBody @Valid final OtpRequest request) {
        String code = otpService.issue(request.getEmail());

        return ResponseEntity.ok(new OtpResponse(
                "A passcode has been sent to " + request.getEmail(),
                properties.isExposeOtp() ? code : null,
                properties.getOtpTtlMinutes()));
    }

    /**
     * POST /auth/verify-otp — exchanges a passcode for a token.
     *
     * A first-time email gets a patient record created for it, so the app can
     * go straight to the profile form rather than dead-ending.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid final VerifyOtpRequest request) {
        if (!otpService.verify(request.getEmail(), request.getOtp())) {
            // One message for every failure, so the response never reveals
            // whether an email is registered or a code is merely expired.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "That passcode is not valid or has expired."));
        }

        // The same form OtpService keys the passcode by, in any locale.
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        boolean newPatient = patientRepository.findByEmailIgnoreCase(email).isEmpty();

        if (newPatient) {
            var patient = new Patient();
            patient.setEmail(email);
            patient.setClinicCode(DEFAULT_CLINIC_CODE);
            patient.setFirstName("");
            patient.setLastName("");
            patient.setGender("");
            patientRepository.save(patient);
            log.info("Created a patient record for {} on first sign-in", email);
        }

        String token = tokenService.issue(email);

        return ResponseEntity.ok(new AuthResponse(
                token, email, properties.getTokenTtlHours() * 3600L, newPatient));
    }
}
