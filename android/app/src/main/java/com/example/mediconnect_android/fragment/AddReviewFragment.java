package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.ReviewClient;
import com.example.mediconnect_android.client.ReviewClientImpl;
import com.example.mediconnect_android.databinding.FragmentAddReviewBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Background;


public class AddReviewFragment extends Fragment {

    FragmentAddReviewBinding binding;
    ReviewClient reviewClient;

    public AddReviewFragment() {
        reviewClient = new ReviewClientImpl();
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
            String appointmentId = getArguments().getString("appointmentId");

            binding.doctorName.setText(doctorName);
            binding.doctorSpecialty.setText(doctorSpecialty);

            Glide.with(requireContext())
                    .load(doctorPhoto)
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(binding.profileImage);

            binding.submitButton.setOnClickListener(v -> {
                double score = binding.ratingBar.getRating();
                String description = binding.reviewInput.getText().toString();
                binding.submitButton.setEnabled(false);

                Background.run(
                        () -> isReviewSubmitted(appointmentId, score, description),
                        submitted -> {
                            if (binding == null) {
                                return;
                            }
                            binding.submitButton.setEnabled(true);
                            if (submitted) {
                                DialogUtils.showMessageDialog(getContext(), "Review submitted successfully");
                                FragmentUtils.loadFragment(requireActivity().getSupportFragmentManager(),
                                        R.id.flFragment, new MedicalHistoryFragment());
                            } else {
                                DialogUtils.showMessageDialog(getContext(),
                                        "Review submission failed. Try again later.");
                            }
                        },
                        error -> {
                            if (binding != null) {
                                binding.submitButton.setEnabled(true);
                            }
                            DialogUtils.showMessageDialog(getContext(),
                                    getString(R.string.error_no_server));
                        });
            });
        }
    }

    private boolean isReviewSubmitted(String appointmentId, Double score, String description) {
        return reviewClient.createReview(appointmentId, score, description);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}