package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
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
import com.example.mediconnect_android.util.UnreadMessages;
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
            // Another tab is another list: start it at the top.
            binding.recyclerView.scrollToPosition(0);
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
        // Callbacks and Undo can arrive after the screen has gone.
        if (binding == null || !isAdded()) {
            return;
        }
        List<Notification> shown = shown();
        adapter = new NotificationAdapter(shown, requireContext(), this::open, this::act)
                .withArchiveAction(showingArchived, this::toggleArchive);
        // A new adapter starts at the top. Archiving one message halfway
        // down should not throw the patient back there, so the scroll
        // position is carried across.
        RecyclerView.LayoutManager layout = binding.recyclerView.getLayoutManager();
        Parcelable scroll = layout == null ? null : layout.onSaveInstanceState();
        binding.recyclerView.setAdapter(adapter);
        if (layout != null && scroll != null) {
            layout.onRestoreInstanceState(scroll);
        }

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

        // Every change to the list comes through here, so this is where the
        // bells hear about it. Setting the count rather than asking the
        // server again matters after "Mark all read": the request that marks
        // them may still be on its way, and a fetch could come back first
        // with the old number.
        UnreadMessages.set((int) unread);

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

    /** The Undo bar on screen, dismissed with the view so its Undo cannot outlive it. */
    private Snackbar undoBar;

    /**
     * The message with this id as the list holds it now. A refresh replaces
     * every object in the list, so a callback that arrives afterwards must
     * not change the one it started with: that one is no longer on screen.
     */
    @Nullable
    private Notification current(String id) {
        for (Notification notification : notifications) {
            if (id.equals(notification.getId())) {
                return notification;
            }
        }
        return null;
    }

    /** Archives a message from the inbox, or moves one back from Archived. */
    private void toggleArchive(Notification notification) {
        setArchived(notification.getId(), !notification.isArchived(), true);
    }

    /**
     * The list changes at once, and the server is told. If it refuses, the
     * change is put back and the patient is told. Undo is offered for the
     * first move only: undoing an Undo is just doing it again.
     */
    private void setArchived(String id, boolean archive, boolean offerUndo) {
        Notification shown = current(id);
        if (binding == null || !isAdded() || shown == null) {
            return;
        }
        boolean wasArchived = shown.isArchived();
        boolean wasRead = shown.isRead();
        shown.setArchived(archive);
        if (archive) {
            // Archiving counts as having dealt with it, as on the server.
            shown.setRead(true);
        }
        bind();

        Background.run(
                () -> archive
                        ? notificationClient.archive(id)
                        : notificationClient.unarchive(id),
                done -> {
                    if (binding == null) {
                        return;
                    }
                    if (!Boolean.TRUE.equals(done)) {
                        putBack(id, wasArchived, wasRead);
                        return;
                    }
                    if (offerUndo) {
                        showUndo(getString(archive
                                        ? R.string.messages_archived_one
                                        : R.string.messages_restored_one),
                                v -> setArchived(id, wasArchived, false));
                    }
                },
                error -> {
                    if (binding != null) {
                        putBack(id, wasArchived, wasRead);
                    }
                });
    }

    private void putBack(String id, boolean archived, boolean read) {
        Notification shown = current(id);
        if (shown != null) {
            shown.setArchived(archived);
            shown.setRead(read);
        }
        bind();
        say(R.string.messages_action_failed);
    }

    /**
     * "Clear read": everything already read goes to Archived in one go,
     * leaving the inbox with what still needs the patient. The list moves
     * at once; when the server answers, it is set to what the server
     * actually archived, and Undo brings back exactly those.
     */
    private void clearRead() {
        List<String> readIds = notifications.stream()
                .filter(notification -> !notification.isArchived() && notification.isRead())
                .map(Notification::getId)
                .collect(Collectors.toList());
        if (readIds.isEmpty()) {
            say(R.string.messages_nothing_to_clear);
            return;
        }
        readIds.forEach(id -> current(id).setArchived(true));
        bind();

        Background.run(
                () -> notificationClient.archiveRead(email()),
                ids -> {
                    if (binding == null) {
                        return;
                    }
                    if (ids == null) {
                        readIds.forEach(id -> {
                            Notification shown = current(id);
                            if (shown != null) {
                                shown.setArchived(false);
                            }
                        });
                        bind();
                        say(R.string.messages_action_failed);
                        return;
                    }
                    for (Notification notification : notifications) {
                        if (ids.contains(notification.getId())) {
                            notification.setArchived(true);
                            notification.setRead(true);
                        } else if (readIds.contains(notification.getId())) {
                            notification.setArchived(false);
                        }
                    }
                    bind();
                    if (ids.isEmpty()) {
                        say(R.string.messages_nothing_to_clear);
                        return;
                    }
                    showUndo(getResources().getQuantityString(
                                    R.plurals.messages_cleared, ids.size(), ids.size()),
                            v -> restore(ids));
                },
                error -> {
                    if (binding != null) {
                        load(true);
                    }
                });
    }

    /**
     * Undo for "Clear read": exactly the messages it archived go back. Any
     * the server would not move back are archived again on screen, so the
     * list never claims more than the server holds.
     */
    private void restore(List<String> ids) {
        if (binding == null || !isAdded()) {
            return;
        }
        ids.forEach(id -> {
            Notification shown = current(id);
            if (shown != null) {
                shown.setArchived(false);
            }
        });
        bind();

        Background.run(
                () -> {
                    List<String> refused = new ArrayList<>();
                    for (String id : ids) {
                        if (!Boolean.TRUE.equals(notificationClient.unarchive(id))) {
                            refused.add(id);
                        }
                    }
                    return refused;
                },
                refused -> {
                    if (binding == null || refused.isEmpty()) {
                        return;
                    }
                    refused.forEach(id -> {
                        Notification shown = current(id);
                        if (shown != null) {
                            shown.setArchived(true);
                        }
                    });
                    bind();
                    say(R.string.messages_action_failed);
                },
                error -> {
                    if (binding != null) {
                        load(true);
                    }
                });
    }

    private void showUndo(CharSequence text, View.OnClickListener undo) {
        undoBar = Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setAction(R.string.messages_undo, undo);
        undoBar.show();
    }

    private void say(int text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
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
        // The bar sits on the activity, not on this view, and outlives it.
        // Its Undo would then act on a screen that is no longer there.
        if (undoBar != null) {
            undoBar.dismiss();
            undoBar = null;
        }
        binding = null;
    }
}
