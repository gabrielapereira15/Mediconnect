package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.TimeslotAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentBinding;
import com.example.mediconnect_android.model.Doctor;

import java.util.ArrayList;
import java.util.List;

public class BookAppointmentFragment extends Fragment {

    FragmentBookAppointmentBinding binding;
    AppointmentClient appointmentClient;
    List<String> dateList = new ArrayList<>();
    List<List<String>> timeSlotsList = new ArrayList<>();
    private TimeslotAdapter adapter;

    public BookAppointmentFragment() {
        appointmentClient = new AppointmentMock();
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

            Doctor doctorDetail = appointmentClient.getDoctor(doctorId);
            // Update the UI with doctor's details
            binding.doctorName.setText(doctorDetail.getName());
            binding.doctorSpecialty.setText(doctorSpecialty);
            binding.doctorDescription.setText(doctorDetail.getDescription());
            binding.doctorScore.setText(String.valueOf(doctorDetail.getScore()));
            binding.doctorImage.setImageResource(R.drawable.doctorimage);

            // Simulating the dates and timeslots data
            var schedule = doctorDetail.getSchedule();
            schedule.forEach(docSchedule -> {
                dateList.add(docSchedule.getDate());
                timeSlotsList.add(docSchedule.getTimes());
            });
        }


        // Initialize the RecyclerView
        bindAdapter();
    }

    private void bindAdapter() {
        // Set the layout manager for the RecyclerView
        binding.timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize the adapter
        adapter = new TimeslotAdapter(getContext(), dateList, timeSlotsList);
        binding.timeSlotsRecyclerView.setAdapter(adapter);
    }
}