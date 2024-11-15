package com.example.mediconnect_android.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.MedicalHistoryFragment;
import com.example.mediconnect_android.fragment.ProfileFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationManager {

    private final FragmentManager fragmentManager;
    private final int fragmentContainerId;
    private final MaterialToolbar toolbar;

    // Constructor to initialize FragmentManager and fragment container ID
    public BottomNavigationManager(FragmentManager fragmentManager, int fragmentContainerId, MaterialToolbar toolbar) {
        this.fragmentManager = fragmentManager;
        this.fragmentContainerId = fragmentContainerId;
        this.toolbar = toolbar;
    }

    // Method to set up the BottomNavigationView listener
    public void setupBottomNavigationListener(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.home_fragment) {
                selectedFragment = new HomeFragment();
                setToolbarTitle("Mediconnect");
            } else if (item.getItemId() == R.id.medical_history_fragment) {
                selectedFragment = new MedicalHistoryFragment();
                setToolbarTitle("Medical History");
            } else if (item.getItemId() == R.id.profile_fragment) {
                selectedFragment = new ProfileFragment();
                setToolbarTitle("Profile");
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    // Method to load the selected fragment
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(fragmentContainerId, fragment);
        transaction.commit();
    }

    private void setToolbarTitle(String title) {
        toolbar.setTitle(title);
    }
}
