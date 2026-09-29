package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.schedule.SlotNoLongerAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppointmentApiControllerAdvice {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<AppointmentRequest> handleAppointmentNotFound(AppointmentNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 409, with the reason in the body: the patient asked for something
     * reasonable and someone else simply got there first, so the app can
     * say which time went rather than "please try again later".
     */
    @ExceptionHandler(SlotNoLongerAvailableException.class)
    public ResponseEntity<String> handleSlotTaken(SlotNoLongerAvailableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }
}
