package com.example.mediconnect_android.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mediconnect_android.client.ApiConfig;

/**
 * Remembers who is signed in, and the token that proves it.
 *
 * The session used to be an email and a boolean, with nothing sent to the
 * server — so the API had no way to tell callers apart. It now stores the
 * bearer token issued at sign-in and restores it into {@link ApiConfig} when
 * the app starts, so requests are authenticated from the first screen.
 */
public class SessionManager {

    private static final String PREF_NAME = "UserSession";

    /**
     * Every store that belongs to the signed-in patient.
     *
     * Signing out has to empty all of them. It used to clear only the
     * session, which left the profile, the reminder switches and the
     * half-written answers to a pre-appointment form on the device for
     * whoever signed in next.
     */
    private static final String[] PATIENT_STORES = {
            PREF_NAME,
            "UserProfile",
            "ReminderPrefs",
            VisitReminders.PREFS,
            "PreVisitFormDrafts",
    };

    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_TOKEN = "authToken";
    private static final String KEY_TOKEN_EXPIRES_AT = "authTokenExpiresAt";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        restoreToken();
    }

    /** Stores a completed sign-in and makes the token active. */
    public void createLoginSession(String email, String token, long expiresInSeconds) {
        long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L);

        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
                .apply();

        ApiConfig.setToken(token);
    }

    /**
     * Kept for callers that only have an email — for example the demo mode
     * sign-in, where no server issued a token.
     */
    public void createLoginSession(String email) {
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    /** True when there is a session and its token has not expired. */
    public boolean isLoggedIn() {
        if (!sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return false;
        }
        if (isTokenExpired()) {
            logoutUser();
            return false;
        }
        return true;
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    /** Clears the session locally; there is no server-side session to end. */
    public void logoutUser() {
        // The alarms are not preferences, so emptying the stores would leave
        // them set, and the next person to sign in on this phone would be
        // reminded of this patient's visits. They go first, because the
        // store cleared below is what says which ones there are.
        VisitReminders.cancelAll(context);
        for (String store : PATIENT_STORES) {
            context.getSharedPreferences(store, Context.MODE_PRIVATE).edit().clear().apply();
        }
        ApiConfig.clearToken();
    }

    private boolean isTokenExpired() {
        long expiresAt = sharedPreferences.getLong(KEY_TOKEN_EXPIRES_AT, 0L);
        return expiresAt > 0L && System.currentTimeMillis() >= expiresAt;
    }

    /** Puts a still-valid saved token back into circulation on app start. */
    private void restoreToken() {
        String token = getToken();
        if (token != null && !token.isEmpty() && !isTokenExpired()) {
            ApiConfig.setToken(token);
        }
    }
}
