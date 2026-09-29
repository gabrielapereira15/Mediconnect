package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.DoctorHomeScreenAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.FragmentHomeBinding;
import com.example.mediconnect_android.databinding.ViewQuickActionBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.KeyboardUtils;
import com.example.mediconnect_android.util.WhenLabel;
import com.google.android.material.chip.Chip;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Home (board Main).
 *
 * Leads with the patient's next visit and the one thing it still needs from
 * them, then four ways in and a short list of doctors who are free soon. The
 * previous version opened with a search field, clip-art specialties, a grid
 * of doctors and three rotating adverts — none of which said anything about
 * the person holding the phone.
 */
public class HomeFragment extends Fragment {

    /** Enough to show the strip is scrollable without loading the world. */
    private static final int AVAILABLE_TODAY_LIMIT = 6;

    private FragmentHomeBinding binding;

    private final DoctorClient doctorClient = new DoctorClientImpl();
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();
    private final HealthClient healthClient = new HealthClientImpl();

    private final List<Doctor> doctorList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    /**
     * Home draws its own greeting header, so the shared app bar would be a
     * second title above it.
     */
    @Override
    public void onResume() {
        super.onResume();
        setAppBarVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        setAppBarVisible(true);
    }

    private void setAppBarVisible(boolean visible) {
        View toolbar = requireActivity().findViewById(R.id.materialToolbar);
        if (toolbar != null) {
            toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void init() {
        bindGreeting();
        bindQuickActions();
        bindSearch();

        binding.notificationButton.setOnClickListener(v ->
                show(new NotificationsFragment()));
        binding.seeAllDoctors.setOnClickListener(v -> show(new DoctorsFragment()));
        binding.seeAllCategories.setOnClickListener(v -> show(new DoctorsFragment()));
        binding.healthSummaryCard.setOnClickListener(v -> show(new HealthRecordFragment()));
        binding.noVisitBook.setOnClickListener(v -> show(new DoctorsFragment()));

        binding.recyclerViewDoctors.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        loadDoctors();
        loadNextVisit();
        loadHealthSummary();
    }

    // ---- greeting -------------------------------------------------------

    private void bindGreeting() {
        SharedPreferences prefs = prefs();
        String firstName = prefs.getString("first_name", "");
        String lastName = prefs.getString("last_name", "");

        binding.greeting.setText(greetingForHour(LocalDateTime.now().getHour()));
        binding.greetingName.setText(firstName.isEmpty() ? getString(R.string.nav_profile) : firstName);
        binding.avatarInitials.setText(WhenLabel.initials(firstName + " " + lastName));
    }

    @StringRes
    private int greetingForHour(int hour) {
        if (hour < 12) {
            return R.string.home_greeting_morning;
        }
        return hour < 18 ? R.string.home_greeting_afternoon : R.string.home_greeting_evening;
    }

    // ---- quick actions --------------------------------------------------

    private void bindQuickActions() {
        // The primary one is filled; the rest are tonal, so there is one
        // obvious first move rather than four equal squares.
        bindAction(binding.actionBook, R.drawable.ic_calendar_plus, R.string.home_action_book,
                true, () -> show(new DoctorsFragment()));
        bindAction(binding.actionForms, R.drawable.ic_form, R.string.home_action_forms,
                false, () -> show(new PreAppointmentFormFragment()));
        bindAction(binding.actionRecord, R.drawable.ic_record, R.string.home_action_record,
                false, () -> show(new HealthRecordFragment()));
        bindAction(binding.actionMessages, R.drawable.ic_mail, R.string.home_action_messages,
                false, () -> show(new NotificationsFragment()));
    }

    private void bindAction(ViewQuickActionBinding action, int iconRes, @StringRes int labelRes,
                            boolean primary, Runnable onClick) {
        action.actionIcon.setImageResource(iconRes);
        action.actionLabel.setText(labelRes);
        action.actionTile.setBackgroundResource(
                primary ? R.drawable.tile_brand : R.drawable.tile_brand_soft);
        action.actionIcon.setImageTintList(androidx.core.content.ContextCompat.getColorStateList(
                requireContext(), primary ? R.color.md_on_primary : R.color.md_on_primary_container));

        action.getRoot().setOnClickListener(v -> onClick.run());
        action.getRoot().setContentDescription(getString(labelRes));
    }

    private void bindSearch() {
        binding.searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                KeyboardUtils.hideKeyboard(v, getContext());
                show(DoctorsFragment.withQuery(v.getText().toString()));
                return true;
            }
            return false;
        });
    }

    // ---- next visit -----------------------------------------------------

    private void loadNextVisit() {
        String email = prefs().getString("email", "");

        Background.run(
                () -> appointmentClient.getAppointments(email),
                appointments -> {
                    if (binding == null) {
                        return;
                    }
                    nextUpcoming(appointments).ifPresentOrElse(this::bindNextVisit, this::bindNoVisit);
                },
                error -> {
                    if (binding != null) {
                        bindNoVisit();
                    }
                });
    }

    /** The soonest appointment still ahead of us, by its ISO timestamp. */
    private Optional<Appointment> nextUpcoming(List<Appointment> appointments) {
        if (appointments == null) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return appointments.stream()
                .filter(a -> "UPCOMING".equals(a.getStatus()))
                .filter(a -> WhenLabel.parse(a.getStartsAt()).map(at -> at.isAfter(now)).orElse(false))
                .min(Comparator.comparing(a -> WhenLabel.parse(a.getStartsAt()).orElse(LocalDateTime.MAX)));
    }

    private void bindNextVisit(Appointment appointment) {
        binding.nextVisitCard.setVisibility(View.VISIBLE);
        binding.noVisitCard.setVisibility(View.GONE);

        WhenLabel.parse(appointment.getStartsAt()).ifPresent(at -> {
            // The headline carries the time and the line under it the day,
            // so neither repeats the other.
            binding.nextVisitWhen.setText(WhenLabel.timeWords(at));
            binding.nextVisitDate.setText(WhenLabel.relativeDayWords(requireContext(), at));
        });

        String doctorName = WhenLabel.doctorName(appointment.getDoctor().getName());
        binding.nextVisitDoctor.setText(doctorName);
        binding.nextVisitSpecialty.setText(appointment.getDoctor().getSpecialty());
        binding.nextVisitDoctorInitials.setText(
                WhenLabel.initials(appointment.getDoctor().getName()));

        // One next step, not a row of equal options. Until the backend
        // tracks whether the form is done, the form is always the step
        // before the visit — see the note in the handoff's open questions.
        binding.nextVisitBadge.setVisibility(View.VISIBLE);
        binding.nextVisitBadge.setText(R.string.home_form_pending);
        binding.nextVisitPrimary.setText(R.string.visit_fill_in_form);
        binding.nextVisitPrimary.setOnClickListener(v -> show(new PreAppointmentFormFragment()));
        binding.nextVisitDetails.setOnClickListener(v -> show(new MedicalHistoryFragment()));
    }

    private void bindNoVisit() {
        binding.nextVisitCard.setVisibility(View.GONE);
        binding.noVisitCard.setVisibility(View.VISIBLE);
    }

    // ---- doctors --------------------------------------------------------

    private void loadDoctors() {
        Background.run(
                doctorClient::getDoctors,
                doctors -> {
                    if (binding == null || doctors == null) {
                        return;
                    }
                    doctorList.clear();
                    doctorList.addAll(doctors);
                    bindDoctorStrip();
                    bindSpecialtyChips();
                },
                error -> { /* the strip simply stays empty */ });
    }

    /**
     * Doctors with something free today, or the soonest few when nobody is.
     * A heading that says "Available today" over a list of next-week slots
     * would be a lie, so the heading changes with the content.
     */
    private void bindDoctorStrip() {
        List<Doctor> today = doctorList.stream()
                .filter(d -> WhenLabel.parse(d.getNextAvailableAt())
                        .map(WhenLabel::isToday).orElse(false))
                .limit(AVAILABLE_TODAY_LIMIT)
                .collect(Collectors.toList());

        boolean anyToday = !today.isEmpty();
        List<Doctor> shown = anyToday ? today : doctorList.stream()
                .filter(d -> d.getNextAvailableAt() != null)
                .limit(AVAILABLE_TODAY_LIMIT)
                .collect(Collectors.toList());

        binding.availableDoctorsTitle.setText(
                anyToday ? R.string.home_available_today : R.string.home_available_soon);
        binding.recyclerViewDoctors.setAdapter(
                new DoctorHomeScreenAdapter(shown, getContext()));
    }

    /** The specialties this clinic actually has, not a fixed set of four. */
    private void bindSpecialtyChips() {
        Set<String> specialties = doctorList.stream()
                .map(Doctor::getSpecialty)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        binding.specialtyChips.removeAllViews();
        for (String specialty : specialties) {
            Chip chip = new Chip(requireContext());
            chip.setText(specialty);
            chip.setCheckable(false);
            chip.setChipBackgroundColorResource(R.color.md_surface_container);
            chip.setOnClickListener(v -> show(DoctorsFragment.withSpecialty(specialty)));
            binding.specialtyChips.addView(chip);
        }
    }

    // ---- health record --------------------------------------------------

    private void loadHealthSummary() {
        String email = prefs().getString("email", "");

        Background.run(
                () -> healthClient.getEntries(email),
                entries -> {
                    if (binding == null || entries == null) {
                        return;
                    }
                    bindHealthSummary(entries);
                },
                error -> { /* the card keeps its placeholder text */ });
    }

    private void bindHealthSummary(List<HealthEntry> entries) {
        long allergies = count(entries, HealthEntry.TYPE_ALLERGY);
        long medications = count(entries, HealthEntry.TYPE_MEDICATION);
        long conditions = count(entries, HealthEntry.TYPE_CONDITION);

        if (allergies + medications + conditions == 0) {
            binding.healthSummaryTitle.setText(R.string.home_record_empty);
            binding.healthSummaryCounts.setText(R.string.home_record_empty_body);
            return;
        }
        binding.healthSummaryTitle.setText(R.string.home_record_up_to_date);
        binding.healthSummaryCounts.setText(getString(R.string.home_record_counts,
                (int) allergies, (int) medications, (int) conditions));
    }

    private long count(List<HealthEntry> entries, String type) {
        return entries.stream().filter(e -> type.equals(e.getType()) && e.isActive()).count();
    }

    // ---- plumbing -------------------------------------------------------

    private void show(Fragment fragment) {
        FragmentUtils.loadFragment(
                ((AppCompatActivity) requireActivity()).getSupportFragmentManager(),
                R.id.flFragment, fragment);
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
