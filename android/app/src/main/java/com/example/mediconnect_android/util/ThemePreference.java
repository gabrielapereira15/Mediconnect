package com.example.mediconnect_android.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Light, dark, or whatever the phone is set to.
 *
 * The app followed the system setting and offered no way to override it,
 * which is fine until someone keeps their phone in dark mode but wants to
 * read a medication list in daylight. System stays the default, because it
 * is what most people expect and it respects a schedule they have already
 * set elsewhere.
 *
 * Applied in Application.onCreate as well as when it changes, so the choice
 * survives the app being killed rather than quietly reverting.
 */
public final class ThemePreference {

    private static final String PREFS = "Appearance";
    private static final String KEY_MODE = "themeMode";

    /** Follow the phone. */
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private ThemePreference() {
    }

    /** Reads the saved choice and applies it. Call before any view exists. */
    public static void apply(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightMode(get(context)));
    }

    /** Saves the choice and applies it immediately. */
    public static void set(Context context, int mode) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(nightMode(mode));
    }

    public static int get(Context context) {
        return prefs(context).getInt(KEY_MODE, MODE_SYSTEM);
    }

    private static int nightMode(int mode) {
        return switch (mode) {
            case MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
