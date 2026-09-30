package com.example.mediconnect_android.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.mediconnect_android.R;

/**
 * Shows a visit reminder when its alarm goes off.
 *
 * It used to post whatever the alarm carried, unconditionally, so an alarm
 * that outlived its visit (cancelled, switched off, or set by a patient who
 * has since signed out) still told someone to be at the clinic. Now it asks
 * {@link VisitReminders} first and shows the words stored there. Alarms set
 * before that fix carry no appointment, and nothing can tell whether their
 * visit still stands, so they stay quiet too; opening Visits sets the ones
 * that are still switched on again.
 */
public class Notification extends BroadcastReceiver {

    public static final String channelID = "channel1";
    public static final String appointmentIdExtra = "appointmentIdExtra";
    public static final String slotExtra = "slotExtra";

    @Override
    public void onReceive(Context context, Intent intent) {
        String appointmentId = intent.getStringExtra(appointmentIdExtra);
        int slot = intent.getIntExtra(slotExtra, -1);
        VisitReminders.Alarm alarm = VisitReminders.stillWanted(context, appointmentId, slot);
        if (alarm == null) {
            return;
        }

        // The small icon is a white silhouette because the status bar keeps
        // only its alpha; the brand colour goes in setColor, which the shade
        // uses to tint the icon and the app name.
        android.app.Notification notification = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_stat_mediconnect)
                .setColor(ContextCompat.getColor(context, R.color.md_primary))
                .setContentTitle(alarm.title)
                .setContentText(alarm.body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // A reminder that does nothing when tapped leaves the patient
                // hunting for the app, and one that stays after being read
                // is clutter.
                .setContentIntent(openApp(context))
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            // Keyed like the alarm, so cancelling the visit can take it out
            // of the shade again.
            manager.notify(VisitReminders.requestCode(appointmentId, slot), notification);
        }
    }

    /**
     * Opens the app the way its icon does, which carries on to the
     * patient's screens when they are signed in and to sign-in when not.
     */
    private static PendingIntent openApp(Context context) {
        Intent launch = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) {
            return null;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
