package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.NotificationAdapter;
import com.example.mediconnect_android.databinding.FragmentNotificationsBinding;
import com.example.mediconnect_android.databinding.FragmentProfileBinding;
import com.example.mediconnect_android.model.Notification;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    FragmentNotificationsBinding binding;
    NotificationAdapter adapter;
    List<Notification> notifications = new ArrayList<>();

    public NotificationsFragment() {
        // Required empty public constructor
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());

        try {
            notifications.add(new Notification("Upcoming Appointment",
                    "Your appointment is coming up! Please, to streamline the process fill in the pre-appointment form.",
                    dateFormat.parse("2024-11-14 3:30 PM").getTime()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        try {
            notifications.add(new Notification("We have a new doctor",
                    "Good news, our team is expanding! Book an appointment with our new Gynecologist, Dr. Gabriela Pereira",
                    dateFormat.parse("2024-11-15 3:30 PM").getTime()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        try {
            notifications.add(new Notification("Upcoming Appointment",
                    "It's time to fill in your pre-appointment form.",
                    dateFormat.parse("2024-11-17 12:00 AM").getTime()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

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