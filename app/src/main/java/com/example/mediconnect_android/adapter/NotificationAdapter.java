package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.NotificationItemBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.util.DialogUtils;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    NotificationItemBinding notificationItemBinding;
    private List<Notification> notificationList;
    private Context context;

    public NotificationAdapter(List<Notification> notificationList, Context context) {
        this.notificationList = notificationList;
        this.context = context;
    }

    @NonNull
    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        notificationItemBinding = NotificationItemBinding.inflate(layoutInflater, parent, false);
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
                    DialogUtils.showMessageDialog(context, notification.getBody());
                }
            });

            recyclerItemBinding.buttonClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notificationList.remove(position);
                        notifyItemRemoved(position);
                    }
                }
            });
        }

        public void bindView(Notification notification) {
            this.notification = notification;
            setupMessage(notification);
            recyclerItemBinding.notificationTitle.setText(notification.getTitle());
            recyclerItemBinding.notificationBody.setText(notification.getBody());
        }

        private void setupMessage(Notification notification) {
            recyclerItemBinding.notificationBody.setText(notification.getBody());
            recyclerItemBinding.notificationBody.post(() -> {
                if (recyclerItemBinding.notificationBody.getLineCount() > 2) {
                    String fullText = notification.getBody();

                    int endOfSecondLine = recyclerItemBinding.notificationBody.getLayout()
                            .getLineEnd(1);
                    String truncatedText = fullText.substring(0, endOfSecondLine - 3) + "...";

                    recyclerItemBinding.notificationBody.setText(truncatedText);
                    recyclerItemBinding.buttonRead.setVisibility(View.VISIBLE);
                } else {
                    recyclerItemBinding.buttonRead.setVisibility(View.GONE);
                }
            });

            long currentTimeMillis = System.currentTimeMillis();
            long notificationTimeMillis = notification.getTimestamp();

            long elapsedTimeMillis = currentTimeMillis - notificationTimeMillis;

            long hoursElapsed = elapsedTimeMillis / (1000 * 60 * 60);
            long daysElapsed = hoursElapsed / 24;

            String timeText;
            if (hoursElapsed < 24) {
                timeText = hoursElapsed + " hours ago";
            } else {
                timeText = daysElapsed + " days ago";
            }
            recyclerItemBinding.notificationTime.setText(timeText);
        }
    }
}
