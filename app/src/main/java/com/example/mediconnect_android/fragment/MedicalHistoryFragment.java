package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentMedicalHistoryBinding;
import com.example.mediconnect_android.util.FragmentUtils;
import com.google.android.material.tabs.TabLayout;

public class MedicalHistoryFragment extends Fragment {

    public TabLayout tabLayout;
    public FragmentManager fragmentManager;
    FragmentMedicalHistoryBinding binding;

    public MedicalHistoryFragment() {
        // Required empty public constructor
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

        // Set initial fragment to UpcomingFragment
        UpcomingFragment upcomingFragment = new UpcomingFragment();
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
                    case 0:
                        selectedFragment = new UpcomingFragment();
                        break;
                    case 1:
                        selectedFragment = new CompletedFragment();
                        break;
                    case 2:
                        selectedFragment = new CancelledFragment();
                        break;
                    default:
                        selectedFragment = new UpcomingFragment();
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