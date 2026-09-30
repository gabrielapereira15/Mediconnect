package com.example.mediconnect_android.fragment;

import com.example.mediconnect_android.client.ApiException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.FragmentMedicalHistoryBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.model.WaitlistEntry;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.VisitReminders;
import com.example.mediconnect_android.util.WhenLabel;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Background;

import java.util.Collections;
import java.util.List;

public class MedicalHistoryFragment extends Fragment {

    public FragmentManager fragmentManager;
    FragmentMedicalHistoryBinding binding;
    AppointmentClient appointmentClient;

    /**
     * The list the segments show. It outlives this fragment's view, and a
     * rotation, so a segment rebuilt by the system has something to read.
     */
    private VisitsViewModel visits;

    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    /** The earlier slot the clinic is holding, if there is one. */
    private WaitlistEntry offer;

    public MedicalHistoryFragment() {
        appointmentClient = new AppointmentClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        visits = new ViewModelProvider(this).get(VisitsViewModel.class);
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

        // Fetch off the UI thread, then hand the list to whichever segment
        // is showing. The segment is already in place and redraws itself,
        // so a refresh from Cancelled leaves the patient on Cancelled.
        //
        // No fragment is swapped here. This can land after the activity
        // has saved its state (the app was sent to the background mid-load),
        // and a fragment transaction at that point throws.
        Background.run(
                () -> appointmentClient.getAppointments(email),
                appointments -> {
                    if (binding == null) {
                        return; // the view went away while the request was in flight
                    }
                    finishRefresh();
                    // This is where the app hears that the clinic cancelled
                    // or moved a visit, so the reminders follow the list.
                    // Not when the server was out of reach, though: the
                    // list is then the sample clinic, none of the patient's
                    // real visits are in it, and following it would take
                    // back every reminder they had set.
                    if (appointments != null && !DemoMode.isActive()) {
                        VisitReminders.sync(requireContext(), appointments);
                    }
                    // An empty reply is a list with nothing in it, not
                    // "still loading", so the segment says "no visits".
                    visits.publish(appointments == null
                            ? Collections.emptyList()
                            : appointments);
                    binding.stateView.showContent();
                },
                error -> {
                    finishRefresh();
                    if (binding == null) {
                        return;
                    }
                    binding.stateView.showError(() -> loadAppointments(email));
                });
    }

    /** The visits as last loaded, or null until the first load lands. */
    public List<Appointment> getAppointments() {
        return visits == null ? null : visits.getAppointments().getValue();
    }

    private void init() {
        binding.visitFilter.check(R.id.filter_upcoming);
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
        // A patient who joined from a full day holds no visit, so the offer
        // is for the day they asked for or sooner rather than "instead of"
        // anything, and there is no visit of theirs to keep. Both branches
        // set the button, because the banner is reused for the next offer.
        boolean holdsVisit = offer.holdsVisit();
        binding.offerBanner.offerBody.setText(getString(
                holdsVisit ? R.string.offer_body_held : R.string.offer_body_open,
                WhenLabel.doctorName(offer.getDoctorName()),
                WhenLabel.parse(offer.getOfferedStartsAt())
                        .map(at -> WhenLabel.whenWords(requireContext(), at))
                        .orElse(""),
                dayWords(offer.getCurrentAppointmentDate()),
                holdWords(offer.getOfferExpiresAt())));
        binding.offerBanner.offerKeep.setText(holdsVisit ? R.string.offer_keep : R.string.offer_pass);

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
                    String when = WhenLabel.parse(taken.getOfferedStartsAt())
                            .map(at -> WhenLabel.whenWords(requireContext(), at))
                            .orElse("");
                    DialogUtils.showMessageDialog(getContext(), taken.holdsVisit()
                            ? getString(R.string.offer_taken, when,
                                    dayWords(taken.getCurrentAppointmentDate()))
                            : getString(R.string.offer_taken_open, when));
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

    /**
     * "Keep mine" ("Not this one" when they hold no visit): the slot moves
     * on now, and they stay on the list.
     */
    private void keepMine() {
        WaitlistEntry declined = offer;
        offer = null;
        bindOffer();

        Background.run(
                () -> waitlistClient.decline(email(), declined.getId()),
                done -> DialogUtils.showMessageDialog(getContext(), getString(
                        declined.holdsVisit() ? R.string.offer_kept : R.string.offer_passed,
                        dayWords(declined.getCurrentAppointmentDate()))),
                error -> loadOffers());
    }

    /** "Wed 30 Sep", for the visit the offer would replace or the day asked for. */
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
     *
     * This runs here rather than in onCreateView because only now is
     * everything back after a rotation or a return from a visit. The
     * segment that was open is back in the container, and the buttons have
     * their saved state. The segment in the container decides, and the
     * buttons follow it. The listener goes on last, so putting the buttons
     * back does not count as the patient choosing a segment.
     *
     * A segment can be chosen before the list has arrived, or after it
     * failed to. The segment shows nothing until the list lands, and this
     * screen's own loading or error state covers it in the meantime.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        Fragment showing = fragmentManager.findFragmentById(R.id.fragment_container);
        if (showing != null) {
            binding.visitFilter.check(filterFor(showing));
        } else {
            showFilter(binding.visitFilter.getCheckedButtonId());
        }

        binding.visitFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showFilter(checkedId);
        });
    }

    private void showFilter(int checkedId) {
        Class<? extends Fragment> segment = segmentFor(checkedId);
        // Already there: it redraws itself when the list reloads. Replacing
        // it anyway would also close a review sheet open on top of Past.
        if (segment.isInstance(fragmentManager.findFragmentById(R.id.fragment_container))) {
            return;
        }
        FragmentUtils.swapFragment(fragmentManager, R.id.fragment_container, segment);
    }

    /** Which segment a button shows: Upcoming unless it is one of the other two. */
    static Class<? extends Fragment> segmentFor(int checkedId) {
        if (checkedId == R.id.filter_past) {
            return CompletedFragment.class;
        }
        if (checkedId == R.id.filter_cancelled) {
            return CancelledFragment.class;
        }
        return UpcomingFragment.class;
    }

    /** The button for a segment; segmentFor the other way round. */
    static int filterFor(Fragment segment) {
        if (segment instanceof CompletedFragment) {
            return R.id.filter_past;
        }
        if (segment instanceof CancelledFragment) {
            return R.id.filter_cancelled;
        }
        return R.id.filter_upcoming;
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