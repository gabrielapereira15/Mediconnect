package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.TimeSlotAdapter;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentBinding;
import com.example.mediconnect_android.model.DoctorDetails;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.ArrayList;
import java.util.List;

public class BookAppointmentFragment extends Fragment {

    FragmentBookAppointmentBinding binding;
    DoctorClient doctorClient;
    List<String> dateList = new ArrayList<>();
    List<List<String>> timeSlotsList = new ArrayList<>();
    private TimeSlotAdapter adapter;

    public BookAppointmentFragment() {
        doctorClient = new DoctorClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBookAppointmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        if (getArguments() != null) {
            String doctorId = getArguments().getString("doctorId");
            String doctorPhoto = getArguments().getString("doctorPhoto");
            String doctorSpecialty = getArguments().getString("doctorSpecialty");
            String doctorName = getArguments().getString("doctorName");

            DoctorDetails doctorDetail = doctorClient.getDoctor(doctorId);
            if (doctorDetail == null) {
                DialogUtils.showMessageDialog(getContext(), "Doctor unavailable");
                return;
            }

            // Update the UI with doctor's details
            if (doctorName == null) {
                doctorName = "No name available";
            }

            if (doctorSpecialty == null) {
                doctorSpecialty = "No specialty available";
            }

            String description = doctorDetail.getDescription();
            if (description == null) {
                description = "No description available";
            }

            Double score = doctorDetail.getScore();
            String score_text;
            if (score == null) {
                score_text = "No score available";
            } else {
                score_text = String.valueOf(score);
            }

            binding.doctorName.setText(doctorName);
            binding.doctorSpecialty.setText(doctorSpecialty);
            binding.doctorImage.setImageResource(R.drawable.doctorimage);
            binding.doctorDescription.setText(description);
            binding.doctorScore.setText(score_text);

            // Simulating the dates and timeslots data
            var schedule = doctorDetail.getSchedule();
            schedule.forEach(docSchedule -> {
                dateList.add(docSchedule.getDate());
                timeSlotsList.add(docSchedule.getTimes());
            });
        }

        // Initialize the RecyclerView
        bindAdapter();

        // Set the click listener for the 'Next' button
        binding.btnNext.setOnClickListener(v -> {
            // Navigate to the next fragment
            if (adapter.selectedTimeSlot == null) {
                DialogUtils.showMessageDialog(getContext(), "Please select a time slot");
                return;
            }

            BookAppointmentPatientDetailsFragment detailsFragment = new BookAppointmentPatientDetailsFragment();
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, detailsFragment);
        });
    }

    private void bindAdapter() {
        // Set the layout manager for the RecyclerView
        binding.timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize the adapter
        adapter = new TimeSlotAdapter(getContext(), dateList, timeSlotsList);
        binding.timeSlotsRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
