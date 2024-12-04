package com.example.mediconnect_android.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import com.example.mediconnect_android.R;

public class Notification extends BroadcastReceiver {

    // Constants for notification
    public static final String channelID = "channel1";
    public static final String titleExtra = "titleExtra";
    public static final String messageExtra = "messageExtra";

    // Method called when the broadcast is received
    @Override
    public void onReceive(Context context, Intent intent) {
        // Build the notification using NotificationCompat.Builder
        android.app.Notification notification = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.mediconnect)
                .setContentTitle(intent.getStringExtra(titleExtra))
                .setContentText(intent.getStringExtra(messageExtra))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        // Get the NotificationManager service
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Show the notification using the manager
        if (manager != null) {
            int notificationID = (int) System.currentTimeMillis();
            manager.notify(notificationID, notification);
        }
    }
}
