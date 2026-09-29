package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.DoctorSeeAllAdapter;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.databinding.FragmentDoctorsBinding;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.KeyboardUtils;
import com.example.mediconnect_android.util.WhenLabel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Find a doctor (board P04).
 *
 * Fetches the directory itself rather than being handed a pre-filtered list,
 * which is what let the old "See all" arrive with an empty screen when it
 * was opened without arguments. It also absorbs the separate specialties
 * screen: a specialty is a filter chip here, not a detour through a grid.
 *
 * The list arrives sorted soonest-first from the server, and every filter
 * preserves that order.
 */
public class DoctorsFragment extends Fragment {

    private static final String ARG_SPECIALTY = "specialty";
    private static final String ARG_QUERY = "query";
    private static final String STATE_FILTER = "activeFilter";

    /** Chip tags, so the filter does not depend on the label's wording. */
    private static final String FILTER_ALL = "all";
    private static final String FILTER_TODAY = "today";

    private FragmentDoctorsBinding binding;
    private final DoctorClient doctorClient = new DoctorClientImpl();

    private final List<Doctor> allDoctors = new ArrayList<>();
    private String activeFilter = FILTER_ALL;
    private String query = "";

    public DoctorsFragment() {
    }

    /** Opens the screen already filtered to one specialty. */
    public static DoctorsFragment withSpecialty(String specialty) {
        DoctorsFragment fragment = new DoctorsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SPECIALTY, specialty);
        fragment.setArguments(args);
        return fragment;
    }

    /** Opens the screen with the Home search box's text already applied. */
    public static DoctorsFragment withQuery(String query) {
        DoctorsFragment fragment = new DoctorsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDoctorsBinding.inflate(inflater, container, false);
        init(savedInstanceState);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.doctors_title);
    }

    private void init(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            activeFilter = args.getString(ARG_SPECIALTY, FILTER_ALL);
            query = args.getString(ARG_QUERY, "");
        }
        // A chip the patient chose has to survive a rotation or a switch to
        // dark mode; otherwise the list silently widens back to everyone.
        if (savedInstanceState != null) {
            activeFilter = savedInstanceState.getString(STATE_FILTER, activeFilter);
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.stateView.setContentView(binding.recyclerView);

        if (!query.isEmpty()) {
            binding.searchBar.setText(query);
        }
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                applyFilters();
            }
        });
        binding.searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                KeyboardUtils.hideKeyboard(v, getContext());
                return true;
            }
            return false;
        });

        load();
    }

    private void load() {
        binding.stateView.showLoading();
        Background.run(
                doctorClient::getDoctors,
                doctors -> {
                    if (binding == null) {
                        return;
                    }
                    allDoctors.clear();
                    if (doctors != null) {
                        allDoctors.addAll(doctors);
                    }
                    buildChips();
                    applyFilters();
                },
                error -> {
                    if (binding != null) {
                        binding.stateView.showError(this::load);
                    }
                });
    }

    /**
     * All, Available today, then one chip per specialty the clinic has —
     * built from the data rather than hard-coded, so a new specialty needs
     * no code change.
     */
    private void buildChips() {
        binding.filterChips.removeAllViews();
        addChip(FILTER_ALL, getString(R.string.doctors_filter_all));
        addChip(FILTER_TODAY, getString(R.string.doctors_filter_today));

        Set<String> specialties = allDoctors.stream()
                .map(Doctor::getSpecialty)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String specialty : specialties) {
            addChip(specialty, specialty);
        }
    }

    private void addChip(String tag, String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(tag);
        chip.setChecked(tag.equals(activeFilter));
        chip.setOnClickListener(v -> {
            activeFilter = tag;
            applyFilters();
        });
        binding.filterChips.addView(chip);
    }

    /**
     * Search and the chip together. Both narrow the same list rather than
     * replacing each other, so typing a name inside a specialty behaves the
     * way it looks like it should.
     */
    private void applyFilters() {
        String needle = query.trim().toLowerCase(Locale.getDefault());

        List<Doctor> shown = allDoctors.stream()
                .filter(this::matchesFilter)
                .filter(doctor -> needle.isEmpty()
                        || contains(doctor.getName(), needle)
                        || contains(doctor.getSpecialty(), needle))
                .collect(Collectors.toList());

        binding.recyclerView.setAdapter(new DoctorSeeAllAdapter(shown, getContext()));
        binding.resultCount.setText(shown.size() == 1
                ? getString(R.string.doctors_count_one)
                : getString(R.string.doctors_count, shown.size()));
        binding.resultCount.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);

        binding.stateView.showContentOrEmpty(shown.isEmpty(),
                R.drawable.ic_search,
                R.string.doctors_empty_title,
                R.string.doctors_empty_body);
    }

    private boolean matchesFilter(Doctor doctor) {
        if (FILTER_ALL.equals(activeFilter)) {
            return true;
        }
        if (FILTER_TODAY.equals(activeFilter)) {
            return WhenLabel.parse(doctor.getNextAvailableAt())
                    .map(WhenLabel::isToday)
                    .orElse(false);
        }
        return activeFilter.equals(doctor.getSpecialty());
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null
                && haystack.toLowerCase(Locale.getDefault()).contains(needle);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, activeFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
