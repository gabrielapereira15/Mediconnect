package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    /**
     * Takes nothing, and must stay that way. Android rebuilds this segment
     * by reflection after a rotation, a font-size or dark-mode change, or
     * the process being killed, and a constructor with arguments crashed
     * the Visits tab each time. The list comes from VisitsViewModel instead.
     */
    public CompletedFragment() {
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Tied to the view, not the fragment, so a view rebuilt on the way
        // back from a visit does not end up with two observers.
        VisitsViewModel.forSegment(this).getAppointments()
                .observe(getViewLifecycleOwner(), this::bindAdapter);
    }

    private void init() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // The segments get their list from MedicalHistoryFragment, so a pull
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

    private void bindAdapter(@Nullable List<Appointment> appointments) {
        if (appointments == null) {
            // Not loaded yet, or the load failed. MedicalHistoryFragment is
            // showing its loading or error state over this segment, and an
            // empty state here would claim there are no visits.
            return;
        }

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