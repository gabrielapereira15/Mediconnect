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
import com.example.mediconnect_android.util.ReminderPermissions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.stream.Collectors;

public class UpcomingFragment extends Fragment {

    FragmentUpcomingBinding binding;
    UpcomingAdapter adapter;
    List<Appointment> appointments;

    /** Registered here, as a field, because the Activity Result API needs it before onCreate. */
    private final ReminderPermissions reminderPermissions = new ReminderPermissions(this);

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
                .filter(appointment -> "UPCOMING".equals(appointment.getStatus()))
                .collect(Collectors.toList());

        // An empty tab says what it means and offers the obvious next step,
        // rather than leaving the patient on a dead end.
        binding.stateView.setContentView(binding.recyclerView);
        if (filteredAppointments.isEmpty()) {
            binding.stateView.showEmpty(
                    R.drawable.ic_calendar,
                    R.string.state_no_upcoming_title,
                    R.string.state_no_upcoming_body,
                    R.string.state_book_appointment,
                    this::goToBooking);
        } else {
            binding.stateView.showContent();
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UpcomingAdapter(filteredAppointments, getContext(), reminderPermissions);
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Sends the patient to Home, where the doctor list and booking live.
     *
     * Goes through the bottom navigation rather than swapping the fragment
     * directly, so the selected tab and the toolbar title follow — and because
     * DoctorsFragment needs a doctor list passed in, which an empty
     * appointments tab does not have.
     */
    private void goToBooking() {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (nav != null) {
            nav.setSelectedItemId(R.id.home_fragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}