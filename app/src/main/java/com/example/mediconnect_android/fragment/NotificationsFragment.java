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

        notifications = notificationClient.getNotifications(email);
        bindAdapter();
    }

    private void bindAdapter() {
        if (notifications.isEmpty()) {
            binding.tvEmptyMessage.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);

            if (getActivity() instanceof NotificationBadgeHandler) {
                ((NotificationBadgeHandler) getActivity()).updateNotificationBadgeVisibility(false);
            }
        } else {
            binding.tvEmptyMessage.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);

            if (getActivity() instanceof NotificationBadgeHandler) {
                ((NotificationBadgeHandler) getActivity()).updateNotificationBadgeVisibility(true);
            }
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