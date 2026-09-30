package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.mobile.patient.model.PatientDetailRequest;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createPatient(@RequestBody @Valid final PatientDetailRequest patientDTO) {
        final UUID createdId = patientApiService.create(patientDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updatePatient(@PathVariable(name = "id") final UUID id,
                                              @RequestBody @Valid final PatientDetailResponse patientDTO) {
        patientApiService.update(id, patientDTO);
        return ResponseEntity.ok(id);
    }
}
