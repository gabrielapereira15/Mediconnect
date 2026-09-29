package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.CancelledAdapter;
import com.example.mediconnect_android.databinding.FragmentCancelledBinding;
import com.example.mediconnect_android.model.Appointment;

import java.util.List;
import java.util.stream.Collectors;

public class CancelledFragment extends Fragment {

    FragmentCancelledBinding binding;
    CancelledAdapter adapter;
    List<Appointment> appointments;

    public CancelledFragment(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCancelledBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        bindAdapter();

        // The tabs are handed their data by MedicalHistoryFragment, so a pull
        // asks the parent to re-fetch rather than loading anything here.
        binding.swipeRefresh.setOnRefreshListener(() -> {
            Fragment parent = getParentFragment();
            if (parent instanceof MedicalHistoryFragment) {
                // The spinner stays up until the parent reports back, so the
                // pull actually shows that something is happening.
                ((MedicalHistoryFragment) parent).reload(() -> {
                    if (binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                    }
                });
            } else {
                binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void bindAdapter() {
        List<Appointment> filteredAppointments = appointments.stream()
                .filter(appointment -> "CANCELED".equals(appointment.getStatus()))
                .collect(Collectors.toList());

        // An empty tab now says what it means rather than showing a bare line
        // of text that looked the same as a failure.
        binding.stateView.setContentView(binding.recyclerView);
        binding.stateView.showContentOrEmpty(filteredAppointments.isEmpty(),
                R.drawable.ic_calendar,
                R.string.state_no_cancelled_title,
                R.string.state_no_cancelled_body);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CancelledAdapter(filteredAppointments, getContext());
        binding.recyclerView.setAdapter(adapter);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}