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
import com.example.mediconnect_android.util.Background;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

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

        init();
        loadAppointments(email);

        return view;
    }

    /**
     * Re-fetches the appointments. Called by the tab fragments when the
     * patient pulls to refresh, since they are handed their data rather than
     * loading it themselves.
     */
    public void reload(Runnable onFinished) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        this.onRefreshFinished = onFinished;
        loadAppointments(prefs.getString("email", ""), true);
    }

    /** Stops the pulling tab's spinner, whether the fetch worked or not. */
    private void finishRefresh() {
        if (onRefreshFinished != null) {
            onRefreshFinished.run();
            onRefreshFinished = null;
        }
    }

    private void loadAppointments(String email) {
        loadAppointments(email, false);
    }

    private Runnable onRefreshFinished;

    private void loadAppointments(String email, boolean isRefresh) {
        binding.stateView.setContentView(binding.fragmentContainer);
        // A refresh keeps the current list on screen behind the spinner;
        // replacing it with a skeleton would be a step backwards.
        if (!isRefresh) {
            binding.stateView.showLoading();
        }

        // Fetch off the UI thread, then show the Upcoming tab once it lands.
        Background.run(
                () -> appointmentClient.getAppointments(email),
                appointments -> {
                    if (binding == null) {
                        return; // the view went away while the request was in flight
                    }
                    finishRefresh();
                    appointmentsList = appointments;
                    binding.stateView.showContent();
                    FragmentUtils.loadFragment(fragmentManager, R.id.fragment_container,
                            new UpcomingFragment(appointmentsList));
                },
                error -> {
                    finishRefresh();
                    if (binding == null) {
                        return;
                    }
                    binding.stateView.showError(() -> loadAppointments(email));
                });
    }

    /** The currently visible tab, so a refresh can stop its spinner. */
    public List<Appointment> getAppointments() {
        return appointmentsList;
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