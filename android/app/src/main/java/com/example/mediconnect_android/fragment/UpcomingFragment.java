package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.UpcomingAdapter;
import com.example.mediconnect_android.databinding.FragmentUpcomingBinding;
import com.example.mediconnect_android.model.Appointment;

import java.util.List;
import java.util.stream.Collectors;

public class UpcomingFragment extends Fragment {

    FragmentUpcomingBinding binding;
    UpcomingAdapter adapter;
    List<Appointment> appointments;

    public UpcomingFragment(List<Appointment> appointments) {
        this.appointments = appointments;
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
        bindAdapter();
    }

    private void bindAdapter() {
        List<Appointment> filteredAppointments = appointments.stream()
                .filter(appointment -> "UPCOMING".equals(appointment.getStatus()))
                .collect(Collectors.toList());

        // An empty tab now says what it means rather than showing a bare line
        // of text that looked the same as a failure.
        binding.stateView.setContentView(binding.recyclerView);
        binding.stateView.showContentOrEmpty(filteredAppointments.isEmpty(),
                R.drawable.baseline_event_busy_24,
                R.string.state_no_upcoming_title,
                R.string.state_no_upcoming_body);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UpcomingAdapter(filteredAppointments, getContext());
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}