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

import java.util.List;

public class MedicalHistoryFragment extends Fragment {

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
                    // Re-show whichever segment the patient is on, not
                    // always Upcoming: a refresh from Cancelled used to
                    // throw them back to the top of the list.
                    showFilter(binding.visitFilter.getCheckedButtonId());
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
        setFilterListener();
    }

    /**
     * Three views of one list.
     *
     * The segmented control replaces a TabLayout, which read as the app's
     * own navigation sitting under the app's own navigation. Selecting a
     * segment loads its fragment; the group keeps exactly one selected, so
     * there is no state where none is.
     */
    private void setFilterListener() {
        binding.visitFilter.check(R.id.filter_upcoming);
        binding.visitFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showFilter(checkedId);
        });
    }

    private void showFilter(int checkedId) {
        Fragment selected;
        if (checkedId == R.id.filter_past) {
            selected = new CompletedFragment(appointmentsList);
        } else if (checkedId == R.id.filter_cancelled) {
            selected = new CancelledFragment(appointmentsList);
        } else {
            selected = new UpcomingFragment(appointmentsList);
        }
        FragmentUtils.loadFragment(fragmentManager, R.id.fragment_container, selected);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.nav_visits);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}