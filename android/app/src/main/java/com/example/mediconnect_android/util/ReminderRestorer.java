package com.example.mediconnect_android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Sets the visit reminders again after the phone restarts.
 *
 * Android forgets every alarm on a restart, so a patient who switched their
 * phone off overnight lost the next day's reminders without anything saying
 * so, while the switch on the visit still read "on". An app update is
 * treated the same way, to be sure.
 */
public class ReminderRestorer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // Not exported, so only the system gets here; the check is so that
        // nothing else sent to this receiver by mistake re-arms anything.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            VisitReminders.restoreAll(context);
        }
    }
}
