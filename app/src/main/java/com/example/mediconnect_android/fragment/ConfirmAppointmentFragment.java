package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentConfirmAppointmentBinding;
import com.example.mediconnect_android.util.FragmentUtils;


public class ConfirmAppointmentFragment extends Fragment {

    FragmentConfirmAppointmentBinding binding;
    private static final String ARG_NAME = "name";
    private static final String ARG_DOB = "dob";
    private static final String ARG_PHONE = "phone";
    private static final String ARG_NOTE = "note";

    public ConfirmAppointmentFragment() {
        // Required empty public constructor
    }

    public static ConfirmAppointmentFragment newInstance(String name, String dob, String phone, String note) {
        ConfirmAppointmentFragment fragment = new ConfirmAppointmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_DOB, dob);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_NOTE, note);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConfirmAppointmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();

        return view;
    }

    private void init() {
        // Retrieve the arguments
        if (getArguments() != null) {
            String name = getArguments().getString(ARG_NAME);
            String dob = getArguments().getString(ARG_DOB);
            String phone = getArguments().getString(ARG_PHONE);
            String note = getArguments().getString(ARG_NOTE);

            // Set the values to the views
            binding.tvPatientName.setText(name);
            binding.tvPatientDob.setText(dob);
            binding.tvPatientPhone.setText(phone);
            binding.tvNoteForDoctor.setText(note);
        }

        MedicalHistoryFragment medicalHistoryFragment = new MedicalHistoryFragment();
        FragmentManager fragmentManager = getParentFragmentManager();
        binding.btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmationMessage();
                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, medicalHistoryFragment);
            }
        });
    }

    private void showConfirmationMessage() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Booking Confirmed")
                .setMessage("Your appointment has been successfully booked.")
                .setIcon(R.drawable.baseline_check_circle_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}