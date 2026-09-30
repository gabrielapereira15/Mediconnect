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
import androidx.core.content.ContextCompat;
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
import java.util.Comparator;
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
    private String sort = DoctorFiltersSheet.SORT_SOONEST;
    private boolean onlyFree;

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

    /**
     * The arguments are read here rather than with the view, because the
     * view is built again every time the patient comes back from booking.
     * Reading them there put a patient who had opened the screen on a
     * specialty and then switched to All back on that specialty on the way
     * back — a filter they had already changed their mind about.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            activeFilter = args.getString(ARG_SPECIALTY, FILTER_ALL);
            query = args.getString(ARG_QUERY, "");
        }
        // After the process was killed, what the patient last chose wins
        // over what the screen was opened with.
        if (savedInstanceState != null) {
            activeFilter = savedInstanceState.getString(STATE_FILTER, activeFilter);
            sort = savedInstanceState.getString(DoctorFiltersSheet.KEY_SORT, sort);
            onlyFree = savedInstanceState.getBoolean(DoctorFiltersSheet.KEY_ONLY_FREE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDoctorsBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.doctors_title);
    }

    private void init() {
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

        binding.btnFilters.setOnClickListener(v -> DoctorFiltersSheet.of(sort, onlyFree)
                .show(getChildFragmentManager(), DoctorFiltersSheet.TAG));
        getChildFragmentManager().setFragmentResultListener(DoctorFiltersSheet.RESULT,
                getViewLifecycleOwner(), (key, result) -> {
                    sort = result.getString(DoctorFiltersSheet.KEY_SORT, sort);
                    onlyFree = result.getBoolean(DoctorFiltersSheet.KEY_ONLY_FREE);
                    applyFilters();
                });
        bindFiltersButton();

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
                .filter(doctor -> !onlyFree || doctor.getNextAvailableAt() != null)
                .filter(doctor -> needle.isEmpty()
                        || contains(doctor.getName(), needle)
                        || contains(doctor.getSpecialty(), needle))
                .sorted(order())
                .collect(Collectors.toList());

        binding.recyclerView.setAdapter(new DoctorSeeAllAdapter(shown, getContext()));
        // The order is said in words next to the count, so a list sorted by
        // rating is not mistaken for the soonest-first one.
        binding.resultCount.setText(getResources().getQuantityString(
                R.plurals.doctors_count, shown.size(), shown.size(), getString(sortLabel())));
        bindFiltersButton();
        binding.resultCount.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);

        if (shown.isEmpty()) {
            // The way out of "no results" is the filters that caused it.
            binding.stateView.showEmpty(R.drawable.ic_search,
                    R.string.doctors_empty_title,
                    R.string.doctors_empty_body,
                    R.string.doctors_clear_filters,
                    this::clearFilters);
        } else {
            binding.stateView.showContent();
        }
    }

    private void clearFilters() {
        activeFilter = FILTER_ALL;
        sort = DoctorFiltersSheet.SORT_SOONEST;
        onlyFree = false;
        query = "";
        // Clearing the box runs applyFilters through its watcher.
        binding.searchBar.setText("");
        buildChips();
        applyFilters();
    }

    /**
     * The server sends the list soonest-first, which is kept as it came;
     * the other two orders are applied here.
     */
    private Comparator<Doctor> order() {
        if (DoctorFiltersSheet.SORT_RATING.equals(sort)) {
            return Comparator.comparing(
                    (Doctor doctor) -> doctor.getScore() == null ? 0d : doctor.getScore())
                    .reversed();
        }
        if (DoctorFiltersSheet.SORT_NAME.equals(sort)) {
            return Comparator.comparing(
                    (Doctor doctor) -> doctor.getLastName() == null ? "" : doctor.getLastName(),
                    String.CASE_INSENSITIVE_ORDER);
        }
        return (a, b) -> 0;
    }

    private int sortLabel() {
        if (DoctorFiltersSheet.SORT_RATING.equals(sort)) {
            return R.string.doctors_sorted_rating;
        }
        if (DoctorFiltersSheet.SORT_NAME.equals(sort)) {
            return R.string.doctors_sorted_name;
        }
        return R.string.doctors_sorted_soonest;
    }

    /**
     * Filled while anything in the sheet differs from its defaults, and said
     * in the button's label, so the state is not carried by colour alone.
     */
    private void bindFiltersButton() {
        int active = (onlyFree ? 1 : 0)
                + (DoctorFiltersSheet.SORT_SOONEST.equals(sort) ? 0 : 1);
        binding.btnFilters.setContentDescription(active == 0
                ? getString(R.string.doctors_filters_title)
                : getString(R.string.cd_doctors_filters_active, active));
        binding.btnFilters.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                active == 0 ? R.color.md_surface_sunken : R.color.md_primary));
        binding.btnFilters.setIconTint(ContextCompat.getColorStateList(requireContext(),
                active == 0 ? R.color.md_on_surface : R.color.md_on_primary));
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
        outState.putString(DoctorFiltersSheet.KEY_SORT, sort);
        outState.putBoolean(DoctorFiltersSheet.KEY_ONLY_FREE, onlyFree);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
