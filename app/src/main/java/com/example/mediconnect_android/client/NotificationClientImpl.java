package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.example.mediconnect_android.model.Notification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class NotificationClientImpl implements NotificationClient {
    private final String baseurl = "https://mediconnect-latest.onrender.com";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public List<Notification> getNotifications(String email) {
        String url = baseurl + "/api/mobile/notifications/" + email;
        ApiGenericResponse response = OkHttpClientHelper.get(url);
        if (response.isSuccess()) {
            // Use Gson to deserialize the response body
            Type notificationListType = new TypeToken<List<Notification>>() {
            }.getType();
            Gson gson = new Gson();
            return gson.fromJson(response.getResponseBody(), notificationListType);
        } else {
            Log.e("NotificationClientImpl", "Error getting notifications: " + response.getResponseBody());
            return Collections.emptyList();
        }
    }

    @Override
    public Boolean markAsRead(String notificationId) {
        String url = baseurl + "/api/mobile/notifications/ack/" + notificationId;
        ApiGenericResponse response = OkHttpClientHelper.post(url, "");
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("NotificationClientImpl", "Error marking notification as read: " + response.getResponseBody());
            return false;
        }
    }
}
