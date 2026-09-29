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

    /** Told whenever the answer changes, so a banner can appear at once. */
    private static volatile Runnable onChange;

    private DemoMode() {
    }

    /**
     * Watches for changes.
     *
     * The flag is set from whichever background thread made the request, so
     * the listener has to get itself onto the main thread before touching a
     * view.
     */
    public static void observe(Runnable listener) {
        onChange = listener;
    }

    public static boolean isActive() {
        return active;
    }

    /** Called by the clients when a request could not reach the server. */
    public static void enable() {
        set(true);
    }

    /** Called by the clients after any successful response. */
    public static void disable() {
        set(false);
    }

    private static void set(boolean value) {
        if (active == value) {
            return;
        }
        active = value;
        Runnable listener = onChange;
        if (listener != null) {
            listener.run();
        }
    }
}
