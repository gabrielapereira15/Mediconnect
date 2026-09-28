package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PatientApiControllerAdvice {

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<DoctorResponse> handlePatientNotFound(PatientNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
