package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.mobile.notification.model.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationApiControllerAdvice {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<NotificationRequest> handleNotificationNotFound(NotificationNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
