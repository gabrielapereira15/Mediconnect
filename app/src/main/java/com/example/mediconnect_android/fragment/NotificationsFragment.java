package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.adapter.NotificationAdapter;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.FragmentNotificationsBinding;

import com.example.mediconnect_android.model.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notifications, getContext());
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}