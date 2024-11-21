package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentAddReviewBinding;
import com.example.mediconnect_android.model.DoctorDetails;
import com.example.mediconnect_android.util.DialogUtils;


public class AddReviewFragment extends Fragment {

    FragmentAddReviewBinding binding;

    public AddReviewFragment() {
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
        binding = FragmentAddReviewBinding.inflate(inflater, container, false);
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

            binding.doctorName.setText(doctorName);
            binding.doctorSpecialty.setText(doctorSpecialty);

            Glide.with(requireContext())
                    .load(doctorPhoto)
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(binding.profileImage);

            binding.submitButton.setOnClickListener(v -> {
                DialogUtils.showMessageDialog(getContext(), "Review submitted successfully");
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}