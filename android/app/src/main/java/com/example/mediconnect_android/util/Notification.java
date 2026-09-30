package com.example.mediconnect_android.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.mediconnect_android.R;

public class Notification extends BroadcastReceiver {

    // Constants for notification
    public static final String channelID = "channel1";
    public static final String titleExtra = "titleExtra";
    public static final String messageExtra = "messageExtra";

    // Method called when the broadcast is received
    @Override
    public void onReceive(Context context, Intent intent) {
        // Build the notification using NotificationCompat.Builder. The small
        // icon is a white silhouette because the status bar keeps only its
        // alpha; the brand colour goes in setColor, which the shade uses to
        // tint the icon and the app name.
        android.app.Notification notification = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_stat_mediconnect)
                .setColor(ContextCompat.getColor(context, R.color.md_primary))
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
