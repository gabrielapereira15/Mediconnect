package com.vegs.mediconnect.backoffice.patient;

import com.vegs.mediconnect.backoffice.util.ReferencedException;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/patients", produces = MediaType.APPLICATION_JSON_VALUE)
public class PatientResource {

    private final PatientService patientService;

    public PatientResource(final PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<PatientDTO>> getAllPatients() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getPatient(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(patientService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createPatient(@RequestBody @Valid final PatientDTO patientDTO) {
        final UUID createdId = patientService.create(patientDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updatePatient(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final PatientDTO patientDTO) {
        patientService.update(id, patientDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deletePatient(@PathVariable(name = "id") final UUID id) {
        final ReferencedWarning referencedWarning = patientService.getReferencedWarning(id);
        if (referencedWarning != null) {
            throw new ReferencedException(referencedWarning);
        }
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
