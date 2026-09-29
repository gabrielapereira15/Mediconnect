package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.vegs.mediconnect.auth.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Cancels an appointment.
     *
     * The route carries no email, so the interceptor cannot check ownership
     * from the path the way it does elsewhere — the caller's own email is
     * taken from their token and checked against the appointment instead.
     * Without it, any signed-in patient could cancel a stranger's visit by
     * guessing an id.
     */
    @PutMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable(name = "id") final UUID id,
            final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        appointmentApiService.cancelAppointment(id, String.valueOf(email));
        return ResponseEntity.accepted().build();
    }

}
