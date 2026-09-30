package com.example.mediconnect_android.util;

import android.view.View;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.model.Notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * How many of the patient's messages are still unread, for every bell that
 * shows it.
 *
 * There are two bells: the app bar's, and the one Home draws in its own
 * header because it hides the app bar. Home's never showed its dot and had
 * no label, so TalkBack read it as an unlabelled button; the app bar's was
 * counted once when the app opened and never again, so after "Mark all read"
 * it still said "2 unread". Both read this one count now.
 *
 * It is fetched again whenever the app comes back to the front. The Messages
 * screen sets it directly as messages are read, because it already holds the
 * list, and a refetch there could overtake the request that marks them read
 * and put the old number back.
 *
 * Main thread only: listeners touch views, and every caller is there already.
 */
public final class UnreadMessages {

    /** Told the count once on joining, then every time it is set. */
    public interface Listener {
        void onUnreadCount(int count);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static int count;

    /**
     * Moves on with every fetch and every change. A fetch that set off before
     * the Messages screen marked something read would otherwise land after it
     * and bring back a count the patient has just cleared.
     */
    private static int ticket;

    private UnreadMessages() {
    }

    public static void observe(Listener listener) {
        LISTENERS.add(listener);
        listener.onUnreadCount(count);
    }

    /** Pass the same instance given to observe, or nothing is removed. */
    public static void stopObserving(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static int count() {
        return count;
    }

    /** For a screen that already has the list in hand and knows the answer. */
    public static void set(int unread) {
        ticket++;
        count = unread;
        for (Listener listener : LISTENERS) {
            listener.onUnreadCount(unread);
        }
    }

    /**
     * Asks the server again. A failed request keeps the last count: a bell
     * that went quiet because the network did would say everything was read.
     */
    public static void refresh(NotificationClient client, String email) {
        int asked = startFetch();
        Background.run(
                () -> client.getNotifications(email),
                notifications -> deliver(asked, notifications),
                error -> { /* the bell keeps what it had */ });
    }

    static int startFetch() {
        return ++ticket;
    }

    /** Applies a fetch's answer, unless something newer has arrived since it set off. */
    static void deliver(int asked, List<Notification> notifications) {
        if (asked != ticket) {
            return;
        }
        set(countUnread(notifications));
    }

    /**
     * Unread, not "any". Read messages stay on the list now, so counting
     * them all would leave the bell dotted for ever.
     */
    public static int countUnread(List<Notification> notifications) {
        if (notifications == null) {
            return 0;
        }
        return (int) notifications.stream()
                .filter(n -> n != null && !n.isRead())
                .count();
    }

    /**
     * Puts a count on a bell. The dot is for the eye; a dot says nothing to
     * a screen reader, so the number goes on the button's own label too.
     */
    public static void bindBell(View bell, View dot, int count) {
        dot.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        bell.setContentDescription(count > 0
                ? bell.getContext().getString(R.string.cd_notifications_unread, count)
                : bell.getContext().getString(R.string.cd_notifications));
    }
}
