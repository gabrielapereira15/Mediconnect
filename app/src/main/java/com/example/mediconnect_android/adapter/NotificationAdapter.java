package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.activity.MainActivity;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.NotificationItemBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.util.DialogUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    NotificationItemBinding notificationItemBinding;
    private List<Notification> notificationList;
    private Context context;
    NotificationClient notificationClient;

    public NotificationAdapter(List<Notification> notificationList, Context context) {
        this.notificationList = notificationList;
        this.context = context;
    }

    @NonNull
    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        notificationItemBinding = NotificationItemBinding.inflate(layoutInflater, parent, false);
        notificationClient = new NotificationClientImpl();
        listeners();
        return new NotificationAdapter.ViewHolder(notificationItemBinding);
    }

    private void listeners() {
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.ViewHolder holder, int position) {
        holder.bindView(notificationList.get(position));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        NotificationItemBinding recyclerItemBinding;
        Notification notification;

        public ViewHolder(NotificationItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;

            recyclerItemBinding.buttonRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogUtils.showMessageDialog(context, notification.getMessage());
                }
            });

            recyclerItemBinding.buttonClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    notificationClient.markAsRead(notification.getId());
                    notificationList.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                    if (notificationList.isEmpty()) {
                        ((MainActivity) context).updateNotificationBadgeVisibility(false);
                    }
                }
            });
        }

        public void bindView(Notification notification) {
            this.notification = notification;
            setupMessage(notification);
            recyclerItemBinding.notificationTitle.setText(notification.getTitle());
            recyclerItemBinding.notificationBody.setText(notification.getMessage());
        }

        private void setupMessage(Notification notification) {
            recyclerItemBinding.notificationBody.setText(notification.getMessage());
            recyclerItemBinding.notificationBody.post(() -> {
                if (recyclerItemBinding.notificationBody.getLineCount() > 2) {
                    String fullText = notification.getMessage();

                    int endOfSecondLine = recyclerItemBinding.notificationBody.getLayout()
                            .getLineEnd(1);
                    String truncatedText = fullText.substring(0, endOfSecondLine - 3) + "...";

                    recyclerItemBinding.notificationBody.setText(truncatedText);
                    recyclerItemBinding.buttonRead.setVisibility(View.VISIBLE);
                } else {
                    recyclerItemBinding.buttonRead.setVisibility(View.GONE);
                }
            });

            String timeText = createTimeAgo(notification);

            recyclerItemBinding.notificationTime.setText(timeText);
        }
    }

    private String createTimeAgo(Notification notification) {
        LocalDateTime creationDate = LocalDateTime.parse(notification.getCreationDate());
        LocalDateTime now = ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime();
        long diffInSeconds = Math.abs(now.toEpochSecond(ZoneOffset.UTC) - creationDate.toEpochSecond(ZoneOffset.UTC));
        long days = TimeUnit.DAYS.convert(diffInSeconds, TimeUnit.SECONDS);
        long hours = TimeUnit.HOURS.convert(diffInSeconds, TimeUnit.SECONDS);
        long minutes = TimeUnit.MINUTES.convert(diffInSeconds, TimeUnit.SECONDS);

        if (days > 0) {
            if (days == 1) {
                return "Yesterday";
            }
            return days + " days ago";
        }
        if (hours > 0) {
            if (hours == 1) {
                return "1 hour ago";
            }
            return hours + " hours ago";
        }
        if (minutes > 0) {
            if (minutes == 1) {
                return "1 minute ago";
            }
            return minutes + " minutes ago";
        }
        return "Just now";
    }
}
