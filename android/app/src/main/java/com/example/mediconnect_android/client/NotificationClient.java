package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Notification;

import java.util.List;

public interface NotificationClient {
    List<Notification> getNotifications(String email);

    Boolean markAsRead(String notificationId);

    /** Marks every message this patient can see as read. */
    Boolean markAllRead(String email);
}
