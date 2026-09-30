package com.example.mediconnect_android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.mediconnect_android.model.Notification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The one unread count both bells read.
 *
 * Home's bell never showed it and the app bar's went stale after "Mark all
 * read", so what matters is that every bell hears every change, and that a
 * slow fetch cannot put back a number the patient has just cleared.
 */
public class UnreadMessagesTest {

    private final List<Integer> heard = new ArrayList<>();
    private final UnreadMessages.Listener listener = count -> heard.add(count);

    @Before
    public void startFromNothingUnread() {
        UnreadMessages.set(0);
        UnreadMessages.observe(listener);
        heard.clear();
    }

    @After
    public void stopListening() {
        UnreadMessages.stopObserving(listener);
    }

    @Test
    public void countsOnlyTheUnread() {
        List<Notification> messages = Arrays.asList(message(false), message(true), message(false));

        assertEquals(2, UnreadMessages.countUnread(messages));
    }

    @Test
    public void noListMeansNothingUnread() {
        assertEquals(0, UnreadMessages.countUnread(null));
    }

    @Test
    public void aBellThatArrivesLateHearsTheCountAtOnce() {
        UnreadMessages.set(3);
        List<Integer> late = new ArrayList<>();
        UnreadMessages.Listener lateListener = count -> late.add(count);

        UnreadMessages.observe(lateListener);
        UnreadMessages.stopObserving(lateListener);

        assertEquals(Collections.singletonList(3), late);
    }

    @Test
    public void markingAllReadReachesEveryBell() {
        UnreadMessages.set(2);
        UnreadMessages.set(0);

        assertEquals(Arrays.asList(2, 0), heard);
        assertEquals(0, UnreadMessages.count());
    }

    @Test
    public void aBellThatHasGoneHearsNothingMore() {
        UnreadMessages.stopObserving(listener);

        UnreadMessages.set(4);

        assertTrue(heard.isEmpty());
    }

    @Test
    public void aFetchSetsTheCount() {
        int asked = UnreadMessages.startFetch();

        UnreadMessages.deliver(asked, Arrays.asList(message(false), message(false)));

        assertEquals(2, UnreadMessages.count());
        assertEquals(Collections.singletonList(2), heard);
    }

    @Test
    public void aFetchOvertakenByMarkingReadIsDropped() {
        int asked = UnreadMessages.startFetch();
        // The patient taps "Mark all read" while the fetch is still out.
        UnreadMessages.set(0);

        UnreadMessages.deliver(asked, Arrays.asList(message(false), message(false)));

        assertEquals(0, UnreadMessages.count());
        assertEquals(Collections.singletonList(0), heard);
    }

    @Test
    public void onlyTheLatestOfTwoFetchesCounts() {
        int first = UnreadMessages.startFetch();
        int second = UnreadMessages.startFetch();

        UnreadMessages.deliver(second, Collections.singletonList(message(false)));
        UnreadMessages.deliver(first, Arrays.asList(message(false), message(false), message(false)));

        assertEquals(1, UnreadMessages.count());
    }

    private static Notification message(boolean read) {
        Notification notification = new Notification();
        notification.setRead(read);
        return notification;
    }
}
