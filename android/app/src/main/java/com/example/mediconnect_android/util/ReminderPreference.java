package com.example.mediconnect_android.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Whether a newly booked visit starts with its reminder switched on.
 *
 * Each visit keeps its own switch — this is only what a new one inherits,
 * so a patient who wants reminders does not have to say so on every card.
 * It lives in the same file as those switches, which sign-out clears.
 */
public final class ReminderPreference {

    private static final String PREFS = "ReminderPrefs";
    private static final String KEY_DEFAULT_ON = "remindersDefaultOn";

    private ReminderPreference() {
    }

    /** Off by default: reminders need an exact-alarm permission to work. */
    public static boolean defaultOn(Context context) {
        return prefs(context).getBoolean(KEY_DEFAULT_ON, false);
    }

    public static void setDefaultOn(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_DEFAULT_ON, on).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
