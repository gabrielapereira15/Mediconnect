package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
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

            // Set default or fetched doctor details
            doctorName = doctorName != null ? doctorName : "No name available";
            doctorSpecialty = doctorSpecialty != null ? doctorSpecialty : "No specialty available";
            String description = doctorDetail.getDescription() != null ? doctorDetail.getDescription() : "No description available";
            Double score = doctorDetail.getScore();
            String score_text = score != null ? String.valueOf(score) : "No score available";

            binding.doctorName.setText(doctorName);
            binding.doctorSpecialty.setText(doctorSpecialty);
            binding.doctorDescription.setText(description);
            binding.doctorScore.setText(score_text);

            Glide.with(requireContext())
                    .load(doctorPhoto)
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(binding.doctorImage);

            // Simulating the dates and timeslots data
            var schedule = doctorDetail.getSchedule();
            schedule.forEach(docSchedule -> {
                dateList.add(docSchedule.getDate());
                // Extract the "time" strings from TimeSlot objects
                List<String> timeStrings = new ArrayList<>();
                docSchedule.getTimes().forEach(timeSlot -> timeStrings.add(timeSlot.getTime()));
                timeSlotsList.add(timeStrings);
            });
        }

        // Handle 'Read More' functionality
        binding.readMoreLink.setOnClickListener(v -> {
            // Check the current maxLines and toggle
            if (binding.doctorDescription.getMaxLines() == 2) {
                binding.doctorDescription.setMaxLines(Integer.MAX_VALUE); // Allow full text display
                binding.readMoreLink.setText(R.string.read_less_link);    // Change to 'Read Less'
            } else {
                binding.doctorDescription.setMaxLines(2); // Restrict to 2 lines
                binding.readMoreLink.setText(R.string.read_more_link);    // Change back to 'Read More'
            }
        });

        // Initialize the RecyclerView
        bindAdapter();

        // Set the click listener for the 'Next' button
        binding.btnNext.setOnClickListener(v -> {
            // Navigate to the next fragment
            if (adapter.selectedTimeSlot == null) {
                DialogUtils.showMessageDialog(getContext(), "Please select a time slot");
                return;
            }

            // Create the new fragment instance
            BookAppointmentPatientDetailsFragment detailsFragment = new BookAppointmentPatientDetailsFragment();

            // Create a Bundle to pass data
            Bundle args = new Bundle();
            args.putString("doctorName", binding.doctorName.getText().toString().trim());
            args.putString("doctorSpecialty", binding.doctorSpecialty.getText().toString().trim());
            args.putString("selectedTimeSlot", adapter.selectedTimeSlot);
            args.putString("selectedDate", adapter.selectedDate);

            // Set arguments to the fragment
            detailsFragment.setArguments(args);

            // Navigate to the next fragment
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
