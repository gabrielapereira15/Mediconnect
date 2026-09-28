package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.FragmentConfirmAppointmentBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;


public class ConfirmAppointmentFragment extends Fragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_DOB = "dob";
    private static final String ARG_PHONE = "phone";
    private static final String ARG_NOTE = "note";
    private static final String ARG_DOCTOR_NAME = "doctorName";
    private static final String ARG_DOCTOR_SPECIALTY = "doctorSpecialty";
    private static final String ARG_SELECTED_TIME_SLOT = "selectedTimeSlotTime";
    private static final String ARG_SELECTED_TIME_SLOT_ID = "selectedTimeSlotId";
    private static final String ARG_SELECTED_DATE = "selectedDate";
    FragmentConfirmAppointmentBinding binding;
    AppointmentClient appointmentClient;
    private String selectedTimeSlotId;

    public ConfirmAppointmentFragment() {
        appointmentClient = new AppointmentClientImpl();
    }

    public static ConfirmAppointmentFragment newInstance(String name, String dob, String phone, String note, String doctorName, String doctorSpecialty, String selectedTimeSlot, String selectedTimeSlotId, String selectedDate) {
        ConfirmAppointmentFragment fragment = new ConfirmAppointmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_DOB, dob);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_NOTE, note);
        args.putString(ARG_DOCTOR_NAME, doctorName);
        args.putString(ARG_DOCTOR_SPECIALTY, doctorSpecialty);
        args.putString(ARG_SELECTED_TIME_SLOT, selectedTimeSlot);
        args.putString(ARG_SELECTED_TIME_SLOT_ID, selectedTimeSlotId);
        args.putString(ARG_SELECTED_DATE, selectedDate);
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
            String doctorName = getArguments().getString("doctorName");
            String doctorSpecialty = getArguments().getString("doctorSpecialty");
            String selectedTimeSlot = getArguments().getString("selectedTimeSlotTime");
            selectedTimeSlotId = getArguments().getString("selectedTimeSlotId");
            String selectedDate = getArguments().getString("selectedDate");
            String doctorInfo = doctorName + ", " + doctorSpecialty;

            // Set the values to the views
            binding.tvPatientName.setText(name);
            binding.tvPatientDob.setText(dob);
            binding.tvPatientPhone.setText(phone);
            binding.tvNoteForDoctor.setText(note);
            binding.tvDoctorName.setText(doctorInfo);
            binding.appointmentDate.setText(selectedDate);
            binding.appointmentTime.setText(selectedTimeSlot);
        }

        MedicalHistoryFragment medicalHistoryFragment = new MedicalHistoryFragment();
        FragmentManager fragmentManager = getParentFragmentManager();
        binding.btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!binding.bookingTermsAndConditions.isChecked()) {
                    DialogUtils.showMessageDialog(getContext(), "Please accept the terms and conditions");
                    return;
                }
                if (!isAppointmentCreated(selectedTimeSlotId)) {
                    DialogUtils.showMessageDialog(getContext(), "Appointment not created, please try again later.");
                    return;
                }
                showConfirmationMessage();
                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, medicalHistoryFragment);
            }
        });
    }

    private boolean isAppointmentCreated(String selectedTimeSlotId) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        String jsonString = String.format(
                "{ \"patientEmail\": \"%s\", \"scheduleTimeId\": \"%s\" }",
                email,
                selectedTimeSlotId
        );

        return appointmentClient.createAppointment(jsonString);
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