package com.example.mediconnect_android.fragment;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    FragmentProfileBinding binding;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        Bitmap profileImage = getImageFromInternalStorage(getContext(), "profile_image.jpg");
        if (profileImage != null) {
            binding.profileImage.setImageBitmap(profileImage);
        }

        binding.btnEditProfile.setOnClickListener(v -> {
            loadFragment(getActivity().getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}