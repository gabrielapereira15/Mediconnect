package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.adapter.UpcomingAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.databinding.FragmentUpcomingBinding;
import com.example.mediconnect_android.model.Appointment;

import java.util.ArrayList;
import java.util.List;

public class UpcomingFragment extends Fragment {

    FragmentUpcomingBinding binding;
    UpcomingAdapter adapter;
    AppointmentClient appointmentClient;
    List<Appointment> appointments = new ArrayList<>();

    public UpcomingFragment() {
        appointmentClient = new AppointmentMock();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpcomingBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        appointments.addAll(appointmentClient.getAppointments(1));
        bindAdapter();
    }

    private void bindAdapter() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UpcomingAdapter(appointments, getContext());
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}