package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ordered first because the error-handling starter adds its own advice with
 * a catch-all Throwable handler, and between two unordered advices the one
 * registered first wins. Left to chance, a refused health card could come
 * back as its generic 500 instead of the 400 below. Both handlers here match
 * only their own exceptions, so going first takes nothing else from it.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
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
