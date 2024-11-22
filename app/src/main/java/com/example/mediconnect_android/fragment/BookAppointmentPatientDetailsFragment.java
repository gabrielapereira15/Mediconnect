package com.example.mediconnect_android.fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentPatientDetailsBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.Calendar;

public class BookAppointmentPatientDetailsFragment extends Fragment {

    private FragmentBookAppointmentPatientDetailsBinding binding;

    public BookAppointmentPatientDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBookAppointmentPatientDetailsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        listeners();
    }

    private void listeners() {
        ConfirmAppointmentFragment confirmFragment = new ConfirmAppointmentFragment();
        FragmentManager fragmentManager = getParentFragmentManager();

        binding.rgPatientSelection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == binding.rbForSelf.getId()) {
                    binding.layoutSelfDetails.setVisibility(View.VISIBLE);
                    binding.layoutOtherDetails.setVisibility(View.GONE);

                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
                    String first_name = sharedPreferences.getString("first_name", "");
                    String last_name = sharedPreferences.getString("last_name", "");
                    String fullName = first_name + " " + last_name;
                    String dob = sharedPreferences.getString("dob", "");
                    String phoneNumber = sharedPreferences.getString("phone_number", "");

                    binding.tvPatientName.setText(fullName);
                    binding.tvPatientDob.setText(dob);
                    binding.tvPatientPhone.setText(phoneNumber);

                } else if (checkedId == binding.rbForAnother.getId()) {
                    binding.layoutSelfDetails.setVisibility(View.GONE);
                    binding.layoutOtherDetails.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.etDobOther.setOnClickListener(v -> showDatePickerDialog());

        binding.btnConfirmSelf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.tvPatientName.getText().toString();
                String dob = binding.tvPatientDob.getText().toString();
                String phoneNumber = binding.tvPatientPhone.getText().toString();
                String noteForDoctor = binding.etSelfNote.getText().toString();

                if (isInfoPatientValid()) {
                    DialogUtils.showMessageDialog(getContext(), "Please fill in all the required fields");
                    return;
                }

                // Use the newInstance method to pass data
                ConfirmAppointmentFragment confirmFragment = ConfirmAppointmentFragment.newInstance(
                        name, dob, phoneNumber, noteForDoctor,
                        requireArguments().getString("doctorName"),
                        requireArguments().getString("doctorSpecialty"),
                        requireArguments().getString("selectedTimeSlotTime"),
                        requireArguments().getString("selectedTimeSlotId"),
                        requireArguments().getString("selectedDate")
                );

                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, confirmFragment);
            }
        });

        binding.btnConfirmOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInfoPatientValid()) {
                    DialogUtils.showMessageDialog(getContext(), "Please fill in all the required fields");
                    return;
                }
                String firstName = binding.etFirstName.getText().toString();
                String lastName = binding.etLastName.getText().toString();
                String dob = binding.etDobOther.getText().toString();
                String phoneNumber = binding.tvPhoneNumber.getText().toString();
                String additionalNotes = binding.etNoteOther.getText().toString();
                String fullName = firstName + " " + lastName;

                // Use the newInstance method to pass data
                ConfirmAppointmentFragment confirmFragment = ConfirmAppointmentFragment.newInstance(
                        fullName, dob, phoneNumber, additionalNotes,
                        requireArguments().getString("doctorName"),
                        requireArguments().getString("doctorSpecialty"),
                        requireArguments().getString("selectedTimeSlotTime"),
                        requireArguments().getString("selectedTimeSlotId"),
                        requireArguments().getString("selectedDate")
                );

                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, confirmFragment);
            }
        });
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    @SuppressLint("DefaultLocale") String formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    binding.etDobOther.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }


    private boolean isInfoPatientValid() {
        if (binding.rbForSelf.isChecked()) {
            return false;
        } else if (binding.rbForAnother.isChecked()) {
            String firstName = binding.etFirstName.getText().toString().trim();
            String lastName = binding.etLastName.getText().toString().trim();
            String dob = binding.etDobOther.getText().toString().trim();
            String phoneNumber = binding.tvPhoneNumber.getText().toString().trim();

            if (firstName.isEmpty()) {
                binding.etFirstName.setError("First name is required");
                return true;
            }
            if (lastName.isEmpty()) {
                binding.etLastName.setError("Last name is required");
                return true;
            }
            if (dob.isEmpty()) {
                binding.etDobOther.setError("Date of birth is required");
                return true;
            }
            if (phoneNumber.isEmpty()) {
                binding.tvPhoneNumber.setError("Phone number is required");
                return true;
            }
            return false;
        }
        return true;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
