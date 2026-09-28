package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.data.DemoData;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.model.Notification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class NotificationClientImpl implements NotificationClient {

    @Override
    public List<Notification> getNotifications(String email) {
        String url = ApiConfig.url("/api/mobile/notifications/") + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type notificationListType = new TypeToken<List<Notification>>() {
            }.getType();
            Gson gson = new Gson();
            DemoMode.disable();
            return gson.fromJson(response.getResponseBody(), notificationListType);
        } else {
            Log.e("NotificationClientImpl", "Error getting notifications: " + response.getResponseBody());
            if (OkHttpClientHelper.isOffline(response)) {
                DemoMode.enable();
                return DemoData.notifications();
            }
            throw new ApiException(response.getStatus(), "Could not load notifications");
        }
    }

    @Override
    public Boolean markAsRead(String notificationId) {
        String url = ApiConfig.url("/api/mobile/notifications/ack/") + notificationId;
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("NotificationClientImpl", "Error marking notification as read: " + response.getResponseBody());
            return false;
        }
    }
}
