package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.FragmentMedicalHistoryBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.util.FragmentUtils;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Objects;

public class MedicalHistoryFragment extends Fragment {

    public TabLayout tabLayout;
    public FragmentManager fragmentManager;
    FragmentMedicalHistoryBinding binding;
    AppointmentClient appointmentClient;
    List<Appointment> appointmentsList;

    public MedicalHistoryFragment() {
        appointmentClient = new AppointmentClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMedicalHistoryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        fragmentManager = getChildFragmentManager();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");

        appointmentsList = appointmentClient.getAppointments(email);

        // Set initial fragment to UpcomingFragment
        UpcomingFragment upcomingFragment = new UpcomingFragment(appointmentsList);
        FragmentUtils.loadFragment(fragmentManager, R.id.fragment_container, upcomingFragment);

        init();

        return view;
    }

    private void init() {
        setTabLayout();
    }

    private void setTabLayout() {
        tabLayout = binding.tabLayout;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selectedFragment;
                switch (tab.getPosition()) {
                    case 1:
                        selectedFragment = new CompletedFragment(appointmentsList);
                        break;
                    case 2:
                        selectedFragment = new CancelledFragment(appointmentsList);
                        break;
                    case 0:
                    default:
                        selectedFragment = new UpcomingFragment(appointmentsList);
                        break;
                }

                FragmentUtils.loadFragment(fragmentManager, R.id.fragment_container, selectedFragment);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}