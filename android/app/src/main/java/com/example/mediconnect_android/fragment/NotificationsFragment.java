package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.NotificationAdapter;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.FragmentNotificationsBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.util.Background;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Messages (board P13).
 *
 * The clinic talking to the patient: an earlier slot, a reminder, news.
 * Called Messages rather than Notifications because that is what they are —
 * a system tray is something you dismiss, and these are worth keeping.
 *
 * Reading one used to remove it from the list: the server filtered
 * acknowledged messages out, so glancing at an offer lost it. They stay
 * now, marked read, and one button clears the lot.
 */
public class NotificationsFragment extends Fragment {

    private static final String KIND_WAITLIST_OFFER = "WAITLIST_OFFER";

    private FragmentNotificationsBinding binding;
    private final NotificationClient notificationClient = new NotificationClientImpl();
    private final List<Notification> notifications = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.messages_title);
    }

    private void init() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.stateView.setContentView(binding.recyclerView);
        binding.swipeRefresh.setOnRefreshListener(() -> load(true));
        binding.btnMarkAllRead.setOnClickListener(v -> markAllRead());

        load(false);
    }

    // ---- loading ----------------------------------------------------------

    private void load(boolean isRefresh) {
        // On a refresh the existing list stays put behind the spinner.
        if (!isRefresh) {
            binding.stateView.showLoading();
        }

        Background.run(
                () -> notificationClient.getNotifications(email()),
                loaded -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    notifications.clear();
                    if (loaded != null) {
                        notifications.addAll(loaded);
                    }
                    bind();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    binding.stateView.showError(() -> load(false));
                });
    }

    private void bind() {
        binding.recyclerView.setAdapter(new NotificationAdapter(
                notifications, requireContext(), this::open, this::act));

        long unread = notifications.stream().filter(n -> !n.isRead()).count();
        binding.unreadCount.setText(unread == 0
                ? getString(R.string.messages_all_read)
                : getString(R.string.messages_unread, (int) unread));
        binding.btnMarkAllRead.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
        binding.messagesHeader.setVisibility(notifications.isEmpty() ? View.GONE : View.VISIBLE);

        updateBellBadge(unread > 0);

        binding.stateView.showContentOrEmpty(notifications.isEmpty(),
                R.drawable.ic_bell,
                R.string.state_no_notifications_title,
                R.string.state_no_notifications_body);
    }

    // ---- acting on one -----------------------------------------------------

    /**
     * Opening a message marks it read, which is what opening means. The
     * list is not refetched for it: the row is already on screen and a
     * whole reload to grey one dot would be a visible jolt.
     */
    private void open(Notification notification) {
        if (notification.isRead()) {
            return;
        }
        notification.setRead(true);
        bind();

        Background.run(
                () -> notificationClient.markAsRead(notification.getId()),
                marked -> { /* the row already shows it */ },
                error -> { /* it will still be unread next time, which is honest */ });
    }

    /** The button inside a message, which depends on what kind it is. */
    private void act(Notification notification) {
        open(notification);

        // A reminder about a visit whose form is still due opens that form.
        // Anything else leads to the visit list, through the tab rather
        // than loading its fragment behind a lit-up Profile.
        if (notification.hasFormToFill()) {
            com.example.mediconnect_android.util.FragmentUtils.loadFragment(
                    getParentFragmentManager(), R.id.flFragment,
                    PreAppointmentFormFragment.of(notification.getAppointmentId()));
            return;
        }
        goToTab(R.id.visits_fragment);
    }

    private void markAllRead() {
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        bind();

        Background.run(
                () -> notificationClient.markAllRead(email()),
                marked -> { /* the list already shows it */ },
                error -> load(true));
    }

    // ---- plumbing -----------------------------------------------------------

    private void updateBellBadge(boolean hasUnread) {
        if (getActivity() instanceof NotificationBadgeHandler) {
            ((NotificationBadgeHandler) getActivity())
                    .updateNotificationBadgeVisibility(hasUnread);
        }
    }

    private void goToTab(int itemId) {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (nav != null) {
            nav.setSelectedItemId(itemId);
        }
    }

    private String email() {
        return requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface NotificationBadgeHandler {
        void updateNotificationBadgeVisibility(boolean visible);
    }
}
