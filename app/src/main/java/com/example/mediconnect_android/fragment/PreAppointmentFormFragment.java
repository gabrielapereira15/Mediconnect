package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentPreAppointmentFormBinding;
import com.example.mediconnect_android.databinding.FragmentSettingsBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

public class PreAppointmentFormFragment extends Fragment {

    FragmentPreAppointmentFormBinding binding;

    public PreAppointmentFormFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPreAppointmentFormBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        binding.btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                // Proceed with form submission
                DialogUtils.showMessageDialog(getContext(), "Form Submitted Successfully");
                FragmentUtils.loadFragment(getActivity().getSupportFragmentManager(), R.id.flFragment, new HomeFragment());
            } else {
                // Show a general error message
                DialogUtils.showMessageDialog(getContext(), "Please fill in all required fields.");
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate Surgery Question
        if (binding.radioGroupSurgery.getCheckedRadioButtonId() == -1) {
            binding.radioGroupSurgery.requestFocus();
            isValid = false;
        }

        // Validate Smoke Question
        if (binding.radioGroupSmoke.getCheckedRadioButtonId() == -1) {
            binding.radioGroupSmoke.requestFocus();
            isValid = false;
        }

        // Validate Alcohol Question
        if (binding.radioGroupAlcohol.getCheckedRadioButtonId() == -1) {
            binding.radioGroupAlcohol.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}