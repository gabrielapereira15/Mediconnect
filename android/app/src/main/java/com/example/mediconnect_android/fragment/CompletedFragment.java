package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.CompletedAdapter;
import com.example.mediconnect_android.databinding.FragmentCompletedBinding;
import com.example.mediconnect_android.model.Appointment;

import java.util.List;
import java.util.stream.Collectors;

public class CompletedFragment extends Fragment implements LeaveReviewSheet.OnReviewSent {

    FragmentCompletedBinding binding;
    CompletedAdapter adapter;
    List<Appointment> appointments;

    public CompletedFragment(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCompletedBinding.inflate(inflater, container, false);
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
                .filter(appointment -> "COMPLETED".equals(appointment.getStatus()))
                .collect(Collectors.toList());

        // An empty tab now says what it means rather than showing a bare line
        // of text that looked the same as a failure.
        binding.stateView.setContentView(binding.recyclerView);
        binding.stateView.showContentOrEmpty(filteredAppointments.isEmpty(),
                R.drawable.ic_check_circle,
                R.string.state_no_completed_title,
                R.string.state_no_completed_body);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CompletedAdapter(filteredAppointments, getContext(), this::showReviewSheet);
        binding.recyclerView.setAdapter(adapter);
    }

    private void showReviewSheet(Appointment appointment) {
        LeaveReviewSheet.of(appointment)
                .show(getChildFragmentManager(), LeaveReviewSheet.TAG);
    }

    /**
     * Refetches so the card the review came from shows it.
     *
     * The rating is the server's to report — a review that failed to save
     * should not leave a badge behind claiming it did.
     */
    @Override
    public void onReviewSent() {
        Fragment parent = getParentFragment();
        if (parent instanceof MedicalHistoryFragment) {
            ((MedicalHistoryFragment) parent).reload(() -> {
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}