package com.example.mediconnect_android.util;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;

/**
 * Gets what a reminder needs before a switch is allowed to show "on".
 *
 * Two things, both of which Android can withhold. From Android 13 a new
 * install may not post notifications until the patient says yes; nothing
 * asked, so every reminder was set and then silently dropped while its
 * switch read "on". From Android 12 exact alarms are off until the patient
 * grants them in Settings. The switch now waits for both.
 *
 * Create one as a field of the fragment that shows the switch: the Activity
 * Result API only lets a fragment register before it is created.
 */
public final class ReminderPermissions {

    private static final String TAG = "ReminderPermissions";

    private final Fragment fragment;
    private final ActivityResultLauncher<String> askToNotify;

    /** What to do once the patient answers; null when nothing is waiting. */
    private Runnable onAllowed;
    private Runnable onRefused;

    public ReminderPermissions(Fragment fragment) {
        this.fragment = fragment;
        this.askToNotify = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), this::onNotifyAnswer);
    }

    /**
     * Runs {@code allowed} once reminders can reach the patient, or
     * {@code refused} if they cannot, having said why.
     *
     * Either may run straight away, or after the system's prompt.
     */
    public void ensure(Runnable allowed, Runnable refused) {
        Context context = fragment.getContext();
        if (context == null) {
            refused.run();
            return;
        }
        if (!VisitReminders.notificationsAllowed(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onAllowed = allowed;
                onRefused = refused;
                askToNotify.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            // Before Android 13 there is nothing to ask: notifications were
            // turned off in Settings, and only the patient can undo that.
            explainNotificationsOff(context);
            refused.run();
            return;
        }
        checkExactAlarms(context, allowed, refused);
    }

    private void onNotifyAnswer(Boolean granted) {
        Runnable allowed = onAllowed;
        Runnable refused = onRefused;
        onAllowed = null;
        onRefused = null;

        Context context = fragment.getContext();
        if (allowed == null || context == null) {
            // Asked by an earlier copy of this screen, which the system
            // rebuilt while the prompt was up. The switch there stayed off,
            // and the next tap will find the permission already granted.
            return;
        }
        if (!Boolean.TRUE.equals(granted)) {
            // Also what happens with no prompt at all, once the patient has
            // said no twice: Android stops asking and answers for them.
            explainNotificationsOff(context);
            refused.run();
            return;
        }
        checkExactAlarms(context, allowed, refused);
    }

    private void checkExactAlarms(Context context, Runnable allowed, Runnable refused) {
        if (!VisitReminders.exactAlarmsAllowed(context)) {
            requestExactAlarmPermission(context);
            refused.run();
            return;
        }
        allowed.run();
    }

    private static void explainNotificationsOff(Context context) {
        DialogUtils.showMessageDialog(context,
                context.getString(R.string.reminder_notifications_refused));
    }

    /**
     * Sends the patient to the system screen that grants exact alarms.
     *
     * From Android 12 this permission is off by default, so the first
     * "Remind me" on a modern device always lands here. It used to launch
     * straight from the adapter's context and crash — startActivity needs
     * NEW_TASK when it is not called from an Activity — and it did so
     * without ever saying why the screen had appeared.
     */
    private static void requestExactAlarmPermission(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.reminder_permission_title)
                .setMessage(R.string.reminder_permission_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reminder_permission_open,
                        (dialog, which) -> openExactAlarmSettings(context))
                .show();
    }

    private static void openExactAlarmSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        // Straight to this app's entry rather than the whole list.
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some builds do not ship the screen at all.
            Log.w(TAG, "No exact-alarm settings screen on this device", e);
            DialogUtils.showMessageDialog(context,
                    context.getString(R.string.reminder_permission_unavailable));
        }
    }
}
