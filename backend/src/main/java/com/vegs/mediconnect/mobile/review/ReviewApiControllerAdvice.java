package com.vegs.mediconnect.mobile.review;

import com.vegs.mediconnect.mobile.error.ErrorMessage;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReviewApiControllerAdvice {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleAppointmentAlreadyReviewed(ConstraintViolationException exception) {
        return ResponseEntity
                .badRequest()
                .body(ErrorMessage.builder().message("Appointment already reviewed").build());
    }
}
