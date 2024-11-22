package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.activity.WelcomeActivity;
import com.example.mediconnect_android.databinding.FragmentLogoutBinding;
import com.example.mediconnect_android.util.ActivityUtils;
import com.example.mediconnect_android.util.SessionManager;

public class LogoutFragment extends Fragment {

    FragmentLogoutBinding binding;

    public LogoutFragment() {
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
        binding = FragmentLogoutBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        logoutUser();
    }

    private void logoutUser() {
        // Pass the fragment's context or activity to SessionManager
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.logoutUser();

        // Navigate to LoginActivity
        ActivityUtils.startActivity(requireContext(), WelcomeActivity.class);

        // Optionally, finish the parent activity if you want to completely exit
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}