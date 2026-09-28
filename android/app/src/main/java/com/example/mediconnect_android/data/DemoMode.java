package com.example.mediconnect_android.data;

/**
 * Records whether the app is currently showing sample data.
 *
 * Set the first time a request fails because the server could not be reached,
 * and cleared as soon as one succeeds. Screens read it to show a banner, so
 * demo data is never passed off as live clinic data.
 */
public final class DemoMode {

    private static volatile boolean active;

    private DemoMode() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Called by the clients when a request could not reach the server. */
    public static void enable() {
        active = true;
    }

    /** Called by the clients after any successful response. */
    public static void disable() {
        active = false;
    }
}
