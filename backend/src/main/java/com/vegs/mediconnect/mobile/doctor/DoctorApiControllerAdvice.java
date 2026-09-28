package com.vegs.mediconnect.mobile.doctor;

import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DoctorApiControllerAdvice {

    @ExceptionHandler(DoctorNotFoundException.class)
    public ResponseEntity<DoctorResponse> handleDoctorNotFound(DoctorNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
