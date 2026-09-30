package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.auth.SignedIn;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailRequest;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/mobile/patients", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PatientApiController {

    private final PatientApiService patientApiService;

    @GetMapping("/{email}")
    public ResponseEntity<PatientDetailResponse> getPatientByEmail(@PathVariable(name = "email") final String email) {
        return ResponseEntity.ok(patientApiService.getPatientByEmail(email));
    }

    /**
     * Creates or saves the signed-in patient's own profile. The profile is
     * found by the body's email, so it has to be the token's: otherwise any
     * patient could rewrite somebody else's details, health card included.
     */
    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createPatient(@RequestBody @Valid final PatientDetailRequest patientDTO,
                                              final HttpServletRequest request) {
        patientDTO.setEmail(SignedIn.sameAs(request, patientDTO.getEmail()));
        final UUID createdId = patientApiService.create(patientDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updatePatient(@PathVariable(name = "id") final UUID id,
                                              @RequestBody @Valid final PatientDetailResponse patientDTO,
                                              final HttpServletRequest request) {
        patientApiService.update(id, patientDTO, SignedIn.email(request));
        return ResponseEntity.ok(id);
    }
}
