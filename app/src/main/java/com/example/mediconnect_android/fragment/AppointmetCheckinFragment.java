package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentAppointmetCheckinBinding;
import com.example.mediconnect_android.databinding.FragmentUpcomingBinding;

public class AppointmetCheckinFragment extends Fragment {

    FragmentAppointmetCheckinBinding binding;

    public AppointmetCheckinFragment() {
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
        binding = FragmentAppointmetCheckinBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}