package com.example.mediconnect_android.fragment;

import com.example.mediconnect_android.client.ApiException;
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
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.model.WaitlistEntry;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.WhenLabel;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Background;

import java.util.List;

public class MedicalHistoryFragment extends Fragment {

    public FragmentManager fragmentManager;
    FragmentMedicalHistoryBinding binding;
    AppointmentClient appointmentClient;
    List<Appointment> appointmentsList;

    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    /** The earlier slot the clinic is holding, if there is one. */
    private WaitlistEntry offer;

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
        loadOffers();
    }

    /**
     * An earlier slot the clinic is holding, above everything else.
     *
     * It is the only thing on this screen that expires, so it goes at the
     * top; the rest of the list will still be true tomorrow.
     */
    private void loadOffers() {
        Background.run(
                () -> waitlistClient.list(email()),
                entries -> {
                    if (binding == null || entries == null) {
                        return;
                    }
                    offer = entries.stream()
                            .filter(WaitlistEntry::isOffered)
                            .findFirst()
                            .orElse(null);
                    bindOffer();
                },
                error -> { /* the list is the screen; a missing offer is not an error */ });
    }

    private void bindOffer() {
        if (offer == null) {
            binding.offerBanner.offerCard.setVisibility(View.GONE);
            return;
        }
        binding.offerBanner.offerCard.setVisibility(View.VISIBLE);
        binding.offerBanner.offerBody.setText(getString(R.string.offer_body_held,
                WhenLabel.doctorName(offer.getDoctorName()),
                WhenLabel.parse(offer.getOfferedStartsAt())
                        .map(at -> WhenLabel.whenWords(requireContext(), at))
                        .orElse(""),
                dayWords(offer.getCurrentAppointmentDate()),
                holdWords(offer.getOfferExpiresAt())));

        binding.offerBanner.offerTake.setEnabled(true);
        binding.offerBanner.offerKeep.setEnabled(true);
        binding.offerBanner.offerTake.setOnClickListener(v -> takeIt());
        binding.offerBanner.offerKeep.setOnClickListener(v -> keepMine());
    }

    /**
     * "Take it": the held slot becomes their appointment and the one it
     * replaces is given up, in one step. The list reloads to show it.
     */
    private void takeIt() {
        WaitlistEntry taken = offer;
        binding.offerBanner.offerTake.setEnabled(false);
        binding.offerBanner.offerKeep.setEnabled(false);

        Background.run(
                () -> waitlistClient.accept(email(), taken.getId()),
                appointment -> {
                    if (binding == null) {
                        return;
                    }
                    offer = null;
                    bindOffer();
                    DialogUtils.showMessageDialog(getContext(), getString(R.string.offer_taken,
                            WhenLabel.parse(taken.getOfferedStartsAt())
                                    .map(at -> WhenLabel.whenWords(requireContext(), at))
                                    .orElse(""),
                            dayWords(taken.getCurrentAppointmentDate())));
                    loadAppointments(email());
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    // Most often the hold ran out a moment ago; the server
                    // says so, and the banner goes with it.
                    boolean ended = error instanceof ApiException
                            && ((ApiException) error).getStatus() == 409;
                    DialogUtils.showMessageDialog(getContext(), getString(ended
                            ? R.string.offer_ended
                            : R.string.error_no_server));
                    loadOffers();
                });
    }

    /** "Keep mine": the slot moves on now, and they stay on the list. */
    private void keepMine() {
        WaitlistEntry declined = offer;
        offer = null;
        bindOffer();

        Background.run(
                () -> waitlistClient.decline(email(), declined.getId()),
                done -> DialogUtils.showMessageDialog(getContext(), getString(
                        R.string.offer_kept, dayWords(declined.getCurrentAppointmentDate()))),
                error -> loadOffers());
    }

    /** "Wed 30 Sep", for the visit the offer would replace. */
    private String dayWords(String isoDate) {
        try {
            return java.time.LocalDate.parse(isoDate).format(
                    java.time.format.DateTimeFormatter.ofPattern("EEE d MMM",
                            java.util.Locale.getDefault()));
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** "1 h 42 min" left on the hold, counted from now. */
    private String holdWords(String isoExpiry) {
        long minutes = WhenLabel.parse(isoExpiry)
                .map(at -> java.time.Duration.between(java.time.LocalDateTime.now(), at).toMinutes())
                .orElse(0L);
        if (minutes < 1) {
            return getString(R.string.offer_hold_moments);
        }
        if (minutes < 60) {
            return getString(R.string.offer_hold_minutes, minutes);
        }
        return getString(R.string.offer_hold_hours, minutes / 60, minutes % 60);
    }

    private String email() {
        return requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");
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