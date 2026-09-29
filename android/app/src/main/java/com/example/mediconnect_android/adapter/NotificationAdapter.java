package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.NotificationItemBinding;
import com.example.mediconnect_android.databinding.ViewSectionHeaderBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The messages list (board P13).
 *
 * Grouped into today and earlier, because "when" is the first thing a
 * patient checks about a message and a flat list makes them read every
 * timestamp to find it. A waitlist offer is tinted and carries its own
 * action: it expires, and the others do not.
 */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MESSAGE = 1;

    /** Kinds the server sends, which decide the icon and the action. */
    private static final String KIND_WAITLIST_OFFER = "WAITLIST_OFFER";
    private static final String KIND_APPOINTMENT = "APPOINTMENT";

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final Context context;
    private final List<Object> rows = new ArrayList<>();

    /** Tapping a message, and tapping the action inside one. */
    private final Consumer<Notification> onOpen;
    private final Consumer<Notification> onAction;

    public NotificationAdapter(List<Notification> notifications, Context context,
                               Consumer<Notification> onOpen,
                               Consumer<Notification> onAction) {
        this.context = context;
        this.onOpen = onOpen;
        this.onAction = onAction;
        buildRows(notifications);
    }

    private void buildRows(List<Notification> notifications) {
        if (notifications == null) {
            return;
        }
        List<Notification> today = new ArrayList<>();
        List<Notification> earlier = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (Notification notification : notifications) {
            LocalDate day = WhenLabel.parse(notification.getCreationDate())
                    .map(LocalDateTime::toLocalDate)
                    .orElse(null);
            if (day != null && day.isEqual(now)) {
                today.add(notification);
            } else {
                earlier.add(notification);
            }
        }

        if (!today.isEmpty()) {
            rows.add(context.getString(R.string.messages_today));
            rows.addAll(today);
        }
        if (!earlier.isEmpty()) {
            rows.add(context.getString(R.string.messages_earlier));
            rows.addAll(earlier);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof Notification ? TYPE_MESSAGE : TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(ViewSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new ViewHolder(NotificationItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).bind((Notification) row);
        } else {
            ((HeaderViewHolder) holder).binding.sectionTitle.setText((String) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        final ViewSectionHeaderBinding binding;

        HeaderViewHolder(ViewSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final NotificationItemBinding binding;

        ViewHolder(NotificationItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Notification notification) {
            binding.messageTitle.setText(notification.getTitle());
            binding.messageBody.setText(notification.getMessage());
            binding.messageWhen.setText(whenWords(notification));
            binding.messageUnread.setVisibility(
                    notification.isRead() ? View.INVISIBLE : View.VISIBLE);

            bindKind(notification);

            // Read and unread sounded identical, because the dot is the
            // only thing that differs and a dot says nothing aloud.
            binding.messageCard.setContentDescription(context.getString(
                    notification.isRead() ? R.string.cd_message : R.string.cd_message_unread,
                    notification.getTitle(), notification.getMessage()));
            binding.messageCard.setOnClickListener(v -> onOpen.accept(notification));
        }

        /**
         * A waitlist offer looks different because it behaves differently:
         * it expires, and the only useful thing to do with it is look at it
         * now. Everything else is news the patient can read whenever.
         */
        private void bindKind(Notification notification) {
            String kind = notification.getKind() == null ? "" : notification.getKind();

            if (KIND_WAITLIST_OFFER.equals(kind)) {
                binding.messageCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.md_rating_container));
                binding.messageIcon.setImageResource(R.drawable.ic_hourglass);
                binding.messageIcon.setBackgroundResource(R.drawable.tile_surface);
                binding.messageIcon.setImageTintList(ContextCompat.getColorStateList(
                        context, R.color.md_on_rating_container));
                binding.messageAction.setVisibility(View.VISIBLE);
                binding.messageAction.setText(R.string.messages_see_offer);
                binding.messageAction.setOnClickListener(v -> onAction.accept(notification));
                return;
            }

            binding.messageCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.md_surface));

            if (KIND_APPOINTMENT.equals(kind)) {
                binding.messageIcon.setImageResource(R.drawable.ic_bell);
                binding.messageIcon.setBackgroundResource(R.drawable.tile_blue_soft);
                binding.messageIcon.setImageTintList(ContextCompat.getColorStateList(
                        context, R.color.md_on_secondary_container));
                binding.messageAction.setVisibility(View.VISIBLE);
                binding.messageAction.setText(R.string.messages_see_visits);
                binding.messageAction.setOnClickListener(v -> onAction.accept(notification));
                return;
            }

            binding.messageIcon.setImageResource(R.drawable.ic_megaphone);
            binding.messageIcon.setBackgroundResource(R.drawable.tile_brand_soft);
            binding.messageIcon.setImageTintList(ContextCompat.getColorStateList(
                    context, R.color.md_on_primary_container));
            binding.messageAction.setVisibility(View.GONE);
        }

        /**
         * How long ago, in the units a person would use: minutes for the
         * last hour, hours for today, the day itself after that.
         */
        private String whenWords(Notification notification) {
            LocalDateTime at = WhenLabel.parse(notification.getCreationDate()).orElse(null);
            if (at == null) {
                return "";
            }

            Duration since = Duration.between(at, LocalDateTime.now());
            if (since.isNegative() || since.toMinutes() < 1) {
                return context.getString(R.string.messages_just_now);
            }
            if (since.toHours() < 1) {
                return context.getString(R.string.messages_minutes, since.toMinutes());
            }
            if (at.toLocalDate().isEqual(LocalDate.now())) {
                return context.getString(R.string.messages_hours, since.toHours());
            }
            return at.format(DAY);
        }
    }
}
