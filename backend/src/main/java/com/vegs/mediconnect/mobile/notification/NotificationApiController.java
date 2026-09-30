package com.vegs.mediconnect.mobile.notification;

import com.vegs.mediconnect.auth.AuthInterceptor;
import com.vegs.mediconnect.mobile.notification.model.NotificationResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Marks everything this patient can see as read.
     *
     * The email is in the path so the interceptor checks ownership the way
     * it does for the list itself.
     */
    @PostMapping("/read-all/{email}")
    public ResponseEntity<Void> markAllRead(@PathVariable(name = "email") final String email) {
        notificationApiService.markAllRead(email);
        return ResponseEntity.accepted().build();
    }

    /**
     * Marks one message read.
     *
     * The route carries an id and no email, so the interceptor cannot check
     * ownership from the path; the caller's own email comes from their
     * token and is checked against the message instead.
     */
    /** Puts one message away (P13's swipe to archive). Owner checked from the token. */
    @PostMapping("/archive/{notificationId}")
    public ResponseEntity<Void> archive(@PathVariable(name = "notificationId") final UUID notificationId,
                                        final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        notificationApiService.archive(notificationId, String.valueOf(email));
        return ResponseEntity.accepted().build();
    }

    /** Moves an archived message back to the inbox. Owner checked from the token. */
    @PostMapping("/unarchive/{notificationId}")
    public ResponseEntity<Void> unarchive(@PathVariable(name = "notificationId") final UUID notificationId,
                                          final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        notificationApiService.unarchive(notificationId, String.valueOf(email));
        return ResponseEntity.accepted().build();
    }

    /**
     * "Clear read": archives everything already read. The email is in the
     * path so the interceptor checks ownership; the ids archived come back
     * so the app can undo exactly those.
     */
    @PostMapping("/archive-read/{email}")
    public ResponseEntity<List<UUID>> archiveRead(@PathVariable(name = "email") final String email) {
        return ResponseEntity.ok(notificationApiService.archiveRead(email));
    }

    @PostMapping("/ack/{notificationId}")
    public ResponseEntity<Void> acknowledgeNotification(
            @PathVariable(name = "notificationId") final UUID notificationId,
            final HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        notificationApiService.ackNotification(notificationId, String.valueOf(email));
        return ResponseEntity.accepted().build();
    }

}
