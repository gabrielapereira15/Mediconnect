package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.mobile.notification.model.NotificationResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/mobile/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationApiService notificationApiService;

    @GetMapping("/{email}")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(
            @PathVariable(name = "email") final String email) {
        return ResponseEntity.ok(notificationApiService.getNotifications(email));
    }

    @PostMapping("/ack/{notificationId}")
    public ResponseEntity<Void> acknowledgeNotification(
            @PathVariable(name = "notificationId") final UUID notificationId) {
        notificationApiService.ackNotification(notificationId);
        return ResponseEntity.accepted().build();
    }

}
