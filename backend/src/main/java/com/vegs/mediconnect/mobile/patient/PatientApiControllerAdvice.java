package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PatientApiControllerAdvice {

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<DoctorResponse> handlePatientNotFound(PatientNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 400, with the reason in the body: the app shows it as it is, so the
     * patient learns what to fix on the card rather than that "something
     * went wrong".
     */
    @ExceptionHandler(InvalidHealthCardException.class)
    public ResponseEntity<String> handleInvalidHealthCard(InvalidHealthCardException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
