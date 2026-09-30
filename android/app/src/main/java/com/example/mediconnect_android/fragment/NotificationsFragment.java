package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.NotificationAdapter;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.databinding.FragmentNotificationsBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.model.WaitlistEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Messages (board P13).
 *
 * The clinic talking to the patient: an earlier slot, a reminder, news.
 * Called Messages rather than Notifications because that is what they are —
 * a system tray is something you dismiss, and these are worth keeping.
 *
 * Read and unread look different at a glance, the way a mail inbox does.
 * A message can be archived — swiped sideways, or through its screen-reader
 * action — which takes it out of the inbox without losing it: it is still
 * under Archived, and Undo puts it straight back. "Clear read" archives
 * everything already read in one go. Nothing is ever deleted, because what
 * the clinic said is worth being able to find again.
 */
public class NotificationsFragment extends Fragment {

    private static final String KIND_WAITLIST_OFFER = "WAITLIST_OFFER";
    private static final String STATE_ARCHIVED = "showingArchived";

    private FragmentNotificationsBinding binding;
    private final NotificationClient notificationClient = new NotificationClientImpl();
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    /** Everything the patient has, inbox and archived; the tab decides what shows. */
    private final List<Notification> notifications = new ArrayList<>();
    private boolean showingArchived;
    private NotificationAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showingArchived = savedInstanceState != null && savedInstanceState.getBoolean(STATE_ARCHIVED);
    }

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
        binding.btnClearRead.setOnClickListener(v -> clearRead());

        binding.messageFilter.check(showingArchived ? R.id.filter_archived : R.id.filter_inbox);
        binding.messageFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showingArchived = checkedId == R.id.filter_archived;
            bind();
        });

        new ItemTouchHelper(new SwipeToArchive()).attachToRecyclerView(binding.recyclerView);

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

    private List<Notification> shown() {
        return notifications.stream()
                .filter(notification -> notification.isArchived() == showingArchived)
                .collect(Collectors.toList());
    }

    private void bind() {
        List<Notification> shown = shown();
        adapter = new NotificationAdapter(shown, requireContext(), this::open, this::act)
                .withArchiveAction(showingArchived, this::toggleArchive);
        binding.recyclerView.setAdapter(adapter);

        long unread = notifications.stream().filter(n -> !n.isRead()).count();
        long archived = notifications.stream().filter(Notification::isArchived).count();
        long readInInbox = notifications.stream().filter(n -> !n.isArchived() && n.isRead()).count();

        if (showingArchived) {
            binding.unreadCount.setText(getString(R.string.messages_archived_count, (int) archived));
            binding.btnMarkAllRead.setVisibility(View.GONE);
            binding.btnClearRead.setVisibility(View.GONE);
            binding.messagesHint.setText(R.string.messages_archived_hint);
        } else {
            binding.unreadCount.setText(unread == 0
                    ? getString(R.string.messages_all_read)
                    : getString(R.string.messages_unread, (int) unread));
            binding.btnMarkAllRead.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
            binding.btnClearRead.setVisibility(readInInbox == 0 ? View.GONE : View.VISIBLE);
            binding.messagesHint.setText(R.string.messages_swipe_hint);
        }
        binding.messagesHeader.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
        binding.messagesHint.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);

        updateBellBadge(unread > 0);

        if (!shown.isEmpty()) {
            binding.stateView.showContent();
        } else if (showingArchived) {
            binding.stateView.showEmpty(R.drawable.ic_archive,
                    R.string.messages_none_archived_title,
                    R.string.messages_none_archived_body);
        } else if (archived > 0) {
            // All caught up, and there is somewhere to look for the rest.
            binding.stateView.showEmpty(R.drawable.ic_bell,
                    R.string.messages_caught_up_title,
                    R.string.messages_caught_up_body,
                    R.string.messages_see_archived,
                    () -> binding.messageFilter.check(R.id.filter_archived));
        } else {
            binding.stateView.showEmpty(R.drawable.ic_bell,
                    R.string.state_no_notifications_title,
                    R.string.state_no_notifications_body);
        }
    }

    // ---- reading ------------------------------------------------------------

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

    // ---- archiving ---------------------------------------------------------------

    /**
     * Archives a message from the inbox, or moves one back from Archived.
     * The list changes at once and Undo reverses it; if the server refuses,
     * the change is put back and the patient is told.
     */
    private void toggleArchive(Notification notification) {
        boolean toArchive = !notification.isArchived();
        boolean wasRead = notification.isRead();
        notification.setArchived(toArchive);
        if (toArchive) {
            // Archiving counts as having dealt with it, as on the server.
            notification.setRead(true);
        }
        bind();

        Background.run(
                () -> toArchive
                        ? notificationClient.archive(notification.getId())
                        : notificationClient.unarchive(notification.getId()),
                done -> {
                    if (binding == null) {
                        return;
                    }
                    if (!Boolean.TRUE.equals(done)) {
                        notification.setArchived(!toArchive);
                        notification.setRead(wasRead || !toArchive);
                        bind();
                        Snackbar.make(binding.getRoot(), R.string.messages_action_failed,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Snackbar.make(binding.getRoot(), toArchive
                                    ? R.string.messages_archived_one
                                    : R.string.messages_restored_one, Snackbar.LENGTH_LONG)
                            .setAction(R.string.messages_undo, v -> toggleArchive(notification))
                            .show();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    notification.setArchived(!toArchive);
                    notification.setRead(wasRead || !toArchive);
                    bind();
                    Snackbar.make(binding.getRoot(), R.string.messages_action_failed,
                            Snackbar.LENGTH_LONG).show();
                });
    }

    /**
     * "Clear read": everything already read goes to Archived in one go,
     * leaving the inbox with what still needs the patient. Undo brings back
     * exactly the ones this archived.
     */
    private void clearRead() {
        List<Notification> read = notifications.stream()
                .filter(notification -> !notification.isArchived() && notification.isRead())
                .collect(Collectors.toList());
        if (read.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.messages_nothing_to_clear, Snackbar.LENGTH_SHORT).show();
            return;
        }
        read.forEach(notification -> notification.setArchived(true));
        bind();

        Background.run(
                () -> notificationClient.archiveRead(email()),
                ids -> {
                    if (binding == null) {
                        return;
                    }
                    if (ids == null) {
                        read.forEach(notification -> notification.setArchived(false));
                        bind();
                        Snackbar.make(binding.getRoot(), R.string.messages_action_failed,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Snackbar.make(binding.getRoot(),
                                    getString(R.string.messages_cleared, ids.size()), Snackbar.LENGTH_LONG)
                            .setAction(R.string.messages_undo, v -> restore(ids))
                            .show();
                },
                error -> load(true));
    }

    /** Undo for "Clear read": exactly the messages it archived go back. */
    private void restore(List<String> ids) {
        notifications.stream()
                .filter(notification -> ids.contains(notification.getId()))
                .forEach(notification -> notification.setArchived(false));
        bind();
        Background.run(() -> {
            for (String id : ids) {
                notificationClient.unarchive(id);
            }
        });
    }

    /**
     * Swipe either way to archive (in the inbox) or move back (in
     * Archived). Section headings do not move. Behind the row, an icon
     * says which of the two a swipe will do.
     */
    private class SwipeToArchive extends ItemTouchHelper.SimpleCallback {

        private final ColorDrawable background;
        private final Drawable archiveIcon;
        private final Drawable unarchiveIcon;
        private final int margin;

        SwipeToArchive() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            Context context = requireContext();
            background = new ColorDrawable(ContextCompat.getColor(context, R.color.md_surface_sunken));
            archiveIcon = tinted(context, R.drawable.ic_archive);
            unarchiveIcon = tinted(context, R.drawable.ic_unarchive);
            margin = context.getResources().getDimensionPixelSize(R.dimen.space_xl);
        }

        private Drawable tinted(Context context, int res) {
            Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(context, res).mutate());
            DrawableCompat.setTint(icon, ContextCompat.getColor(context, R.color.md_on_surface_variant));
            return icon;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
            if (adapter == null || adapter.messageAt(holder.getAdapterPosition()) == null) {
                return 0;
            }
            return super.getMovementFlags(recyclerView, holder);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from,
                              @NonNull RecyclerView.ViewHolder to) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
            Notification notification = adapter == null ? null : adapter.messageAt(holder.getAdapterPosition());
            if (notification == null) {
                bind();
                return;
            }
            toggleArchive(notification);
        }

        @Override
        public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder holder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            View item = holder.itemView;
            if (dX != 0) {
                int left = dX > 0 ? item.getLeft() : item.getRight() + (int) dX;
                int right = dX > 0 ? item.getLeft() + (int) dX : item.getRight();
                background.setBounds(left, item.getTop(), right, item.getBottom());
                background.draw(canvas);

                Drawable icon = showingArchived ? unarchiveIcon : archiveIcon;
                int size = icon.getIntrinsicHeight();
                if (Math.abs(dX) > margin + size) {
                    int top = item.getTop() + (item.getHeight() - size) / 2;
                    int iconLeft = dX > 0 ? item.getLeft() + margin : item.getRight() - margin - size;
                    icon.setBounds(iconLeft, top, iconLeft + size, top + size);
                    icon.draw(canvas);
                }
            }
            super.onChildDraw(canvas, recyclerView, holder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    // ---- acting on one -----------------------------------------------------

    /** The button inside a message, which depends on what kind it is. */
    private void act(Notification notification) {
        open(notification);

        // An offer can end between loading this list and tapping it. Ask
        // again before sending the patient to look for a banner that is
        // no longer there.
        if (KIND_WAITLIST_OFFER.equals(notification.getKind())) {
            Background.run(
                    () -> waitlistClient.list(email()),
                    entries -> {
                        if (binding == null) {
                            return;
                        }
                        boolean held = entries != null && entries.stream().anyMatch(WaitlistEntry::isOffered);
                        if (held) {
                            goToTab(R.id.visits_fragment);
                        } else {
                            DialogUtils.showMessageDialog(getContext(), getString(R.string.messages_offer_gone));
                            load(true);
                        }
                    },
                    error -> {
                        if (binding != null) {
                            goToTab(R.id.visits_fragment);
                        }
                    });
            return;
        }

        // A reminder about a visit whose form is still due opens that form.
        // Anything else leads to the visit list, through the tab rather
        // than loading its fragment behind a lit-up Profile.
        if (notification.hasFormToFill()) {
            FragmentUtils.loadFragment(getParentFragmentManager(), R.id.flFragment,
                    PreAppointmentFormFragment.of(notification.getAppointmentId()));
            return;
        }
        goToTab(R.id.visits_fragment);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ARCHIVED, showingArchived);
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
