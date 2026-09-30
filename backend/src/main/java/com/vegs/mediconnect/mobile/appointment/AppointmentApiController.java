package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.vegs.mediconnect.auth.AuthInterceptor;
import com.vegs.mediconnect.auth.SignedIn;
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

    /**
     * Books for the signed-in patient. The body's patientEmail used to decide
     * whose appointment it was, so any patient could book in somebody
     * else's name; it may still be sent, but only as the token's own.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody AppointmentRequest appointmentRequest,
                                                                 final HttpServletRequest request) {
        appointmentRequest.setPatientEmail(SignedIn.sameAs(request, appointmentRequest.getPatientEmail()));
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
     * Tells the clinic the patient has arrived.
     *
     * Same ownership rule as cancelling below: the email comes from the
     * caller's token rather than the path, because the route carries no
     * email for the interceptor to check.
     */
    @PutMapping("/{id}/checkin")
    public ResponseEntity<AppointmentResponse> checkIn(
            @PathVariable(name = "id") final UUID id,
            final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        return ResponseEntity.ok(appointmentApiService.checkIn(id, String.valueOf(email)));
    }

    /** "I will be there", from the visit's checklist (board P09). */
    @PutMapping("/{id}/attendance")
    public ResponseEntity<AppointmentResponse> confirmAttendance(
            @PathVariable(name = "id") final UUID id,
            final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        return ResponseEntity.ok(appointmentApiService.confirmAttendance(id, String.valueOf(email)));
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
