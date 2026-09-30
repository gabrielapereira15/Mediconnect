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
    public Boolean markAllRead(String email) {
        String url = ApiConfig.url("/api/mobile/notifications/read-all/") + email;
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        }
        Log.e("NotificationClientImpl", "Error marking all read: " + response.getResponseBody());
        return false;
    }

    @Override
    public Boolean archive(String notificationId) {
        return post(ApiConfig.url("/api/mobile/notifications/archive/") + notificationId, "archiving");
    }

    @Override
    public Boolean unarchive(String notificationId) {
        return post(ApiConfig.url("/api/mobile/notifications/unarchive/") + notificationId, "unarchiving");
    }

    @Override
    public java.util.List<String> archiveRead(String email) {
        String url = ApiConfig.url("/api/mobile/notifications/archive-read/") + email;
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (!response.isSuccess()) {
            Log.e("NotificationClientImpl", "Error clearing read messages: " + response.getResponseBody());
            return null;
        }
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<String>>() {
        }.getType();
        java.util.List<String> ids = new com.google.gson.Gson().fromJson(response.getResponseBody(), type);
        return ids == null ? new java.util.ArrayList<>() : ids;
    }

    private Boolean post(String url, String what) {
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        }
        Log.e("NotificationClientImpl", "Error " + what + " a message: " + response.getResponseBody());
        return false;
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
