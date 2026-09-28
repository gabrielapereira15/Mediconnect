package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.adapter.NotificationAdapter;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.FragmentNotificationsBinding;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.util.Background;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    FragmentNotificationsBinding binding;
    NotificationAdapter adapter;
    List<Notification> notifications = new ArrayList<>();
    NotificationClient notificationClient;

    public NotificationsFragment() {
        notificationClient = new NotificationClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");

        loadNotifications(email);
    }

    private void loadNotifications(String email) {
        binding.stateView.setContentView(binding.recyclerView);
        binding.stateView.showLoading();

        Background.run(
                () -> notificationClient.getNotifications(email),
                loaded -> {
                    if (binding == null) {
                        return; // the view went away while the request was in flight
                    }
                    notifications = loaded;
                    bindAdapter();
                    binding.stateView.showContentOrEmpty(notifications.isEmpty(),
                            R.drawable.baseline_notifications_off_24,
                            R.string.state_no_notifications_title,
                            R.string.state_no_notifications_body);
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.stateView.showError(() -> loadNotifications(email));
                });
    }

    private void bindAdapter() {
        // StateView owns the empty/content visibility; this only has to keep
        // the toolbar badge in step.
        if (getActivity() instanceof NotificationBadgeHandler) {
            ((NotificationBadgeHandler) getActivity())
                    .updateNotificationBadgeVisibility(!notifications.isEmpty());
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notifications, getContext());
        binding.recyclerView.setAdapter(adapter);
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