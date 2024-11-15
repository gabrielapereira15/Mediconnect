package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentPatientDetailsBinding;
import com.example.mediconnect_android.databinding.FragmentConfirmAppointmentBinding;

public class ConfirmAppointmentFragment extends Fragment {
    
    FragmentConfirmAppointmentBinding binding;
    
    public ConfirmAppointmentFragment() {
        // Required empty public constructor
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
        binding.btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Booking Confirmed", Toast.LENGTH_LONG).show();
                transitionToHome();
            }
        });
    }

    private void transitionToHome() {
        HomeFragment homeFragment = new HomeFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.flFragment, homeFragment)
                .addToBackStack(null)
                .commit();
    }

}