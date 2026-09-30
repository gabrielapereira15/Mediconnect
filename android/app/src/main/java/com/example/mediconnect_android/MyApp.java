package com.example.mediconnect_android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.mediconnect_android.util.ThemePreference;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Before any activity is created, otherwise the first screen shows
        // in the system theme and then flips to the chosen one.
        ThemePreference.apply(this);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelID = "channel1";
            // From resources, since the phone's settings list the channel by
            // this name; "Default Channel" told the patient nothing.
            String channelName = getString(R.string.reminder_channel_name);
            String channelDescription = getString(R.string.reminder_channel_body);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelID, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
