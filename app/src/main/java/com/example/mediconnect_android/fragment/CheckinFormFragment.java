package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentCheckinFormBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

public class CheckinFormFragment extends Fragment implements View.OnClickListener {

    FragmentCheckinFormBinding binding;

    public CheckinFormFragment() {
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
        binding = FragmentCheckinFormBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        listeners();
    }

    private void listeners() {
        binding.confirmButton.setOnClickListener(this);
        binding.cancelButton.setOnClickListener(this);
        binding.btnSubmit.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        if (view == binding.confirmButton) {
            binding.layoutCheckinForm.setVisibility(View.VISIBLE);
        } else if (view == binding.cancelButton) {
            binding.layoutCheckinForm.setVisibility(View.GONE);
            showCancelConfirmationDialog();
        } else if (view == binding.btnSubmit) {
            DialogUtils.showMessageDialog(getContext(), "Form Submitted Successfully");
            FragmentUtils.loadFragment(getActivity().getSupportFragmentManager(), R.id.flFragment, new HomeFragment());
        }
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel the appointment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showCancellationMessage();
                    FragmentUtils.loadFragment(getActivity().getSupportFragmentManager(), R.id.flFragment, new MedicalHistoryFragment());
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void showCancellationMessage() {
        new AlertDialog.Builder(getContext())
                .setTitle("Appointment Cancelled")
                .setMessage("Your appointment has been successfully cancelled.")
                .setIcon(R.drawable.baseline_check_circle_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}