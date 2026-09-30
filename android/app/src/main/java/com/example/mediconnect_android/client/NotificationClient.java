package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Notification;

import java.util.List;

public interface NotificationClient {
    List<Notification> getNotifications(String email);

    Boolean markAsRead(String notificationId);

    /** Marks every message this patient can see as read. */
    Boolean markAllRead(String email);

    /** Puts one message away; it stays under Archived. */
    Boolean archive(String notificationId);

    /** Moves an archived message back to the inbox. */
    Boolean unarchive(String notificationId);

    /**
     * Archives every message already read. Returns the ids it archived, so
     * exactly those can be put back; null when the server could not do it.
     */
    java.util.List<String> archiveRead(String email);
}
