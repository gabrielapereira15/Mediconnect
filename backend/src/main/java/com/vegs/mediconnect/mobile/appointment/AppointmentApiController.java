package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/mobile/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AppointmentApiController {

    private final AppointmentApiService appointmentApiService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody AppointmentRequest appointmentRequest) {
        var appointment = appointmentApiService.create(appointmentRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointment);
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @PathVariable(name = "email") final String email) {
        return ResponseEntity.ok(appointmentApiService.getAppointments(email));
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable(name = "id") final UUID id) {
        appointmentApiService.cancelAppointment(id);
        return ResponseEntity.accepted().build();
    }

}
