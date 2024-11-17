package com.example.mediconnect_android.fragment;

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
import com.example.mediconnect_android.util.FragmentUtils;

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

                    binding.tvPatientName.setText("Name: Gabriela Pereira");
                    binding.tvPatientDob.setText("Date of Birth: 03/15/1993");
                    binding.tvPatientPhone.setText("Phone: +1 234 567 890");

                } else if (checkedId == binding.rbForAnother.getId()) {
                    binding.layoutSelfDetails.setVisibility(View.GONE);
                    binding.layoutOtherDetails.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.btnConfirmSelf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteForDoctor = binding.etSelfNote.getText().toString();
                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, confirmFragment);
            }
        });

        binding.btnConfirmOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = binding.etFirstName.getText().toString();
                String lastName = binding.etLastName.getText().toString();
                String dob = binding.etDobOther.getText().toString();
                String additionalNotes = binding.etNoteOther.getText().toString();
                FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, confirmFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
