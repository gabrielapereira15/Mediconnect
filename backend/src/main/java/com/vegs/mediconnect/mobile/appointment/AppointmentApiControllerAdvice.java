package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppointmentApiControllerAdvice {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<AppointmentRequest> handleAppointmentNotFound(AppointmentNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
