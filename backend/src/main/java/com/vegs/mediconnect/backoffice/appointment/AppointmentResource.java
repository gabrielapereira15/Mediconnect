package com.vegs.mediconnect.backoffice.appointment;

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
@RequestMapping(value = "/api/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
public class AppointmentResource {

    private final AppointmentService appointmentService;

    public AppointmentResource(final AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointment(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(appointmentService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createAppointment(
            @RequestBody @Valid final AppointmentDTO appointmentDTO) {
        final UUID createdId = appointmentService.create(appointmentDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateAppointment(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final AppointmentDTO appointmentDTO) {
        appointmentService.update(id, appointmentDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteAppointment(@PathVariable(name = "id") final UUID id) {
        final ReferencedWarning referencedWarning = appointmentService.getReferencedWarning(id);
        if (referencedWarning != null) {
            throw new ReferencedException(referencedWarning);
        }
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
