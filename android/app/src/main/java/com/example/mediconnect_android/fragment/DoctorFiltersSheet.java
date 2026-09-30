package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.SheetDoctorFiltersBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * The Filters sheet behind Find a doctor's sliders button (board P04).
 *
 * Answers through a fragment result rather than a callback interface, so a
 * rotation with the sheet open does not leave it talking to a list that no
 * longer exists.
 */
public class DoctorFiltersSheet extends BottomSheetDialogFragment {

    public static final String TAG = "DoctorFiltersSheet";
    public static final String RESULT = "doctorFilters";
    public static final String KEY_SORT = "sort";
    public static final String KEY_ONLY_FREE = "onlyFree";

    public static final String SORT_SOONEST = "soonest";
    public static final String SORT_RATING = "rating";
    public static final String SORT_NAME = "name";

    private SheetDoctorFiltersBinding binding;
    private boolean onlyFree;

    public static DoctorFiltersSheet of(String sort, boolean onlyFree) {
        DoctorFiltersSheet sheet = new DoctorFiltersSheet();
        Bundle args = new Bundle();
        args.putString(KEY_SORT, sort);
        args.putBoolean(KEY_ONLY_FREE, onlyFree);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetDoctorFiltersBinding.inflate(inflater, container, false);
        Bundle args = requireArguments();

        binding.sortGroup.check(sortButton(args.getString(KEY_SORT, SORT_SOONEST)));

        onlyFree = savedInstanceState != null
                ? savedInstanceState.getBoolean(KEY_ONLY_FREE)
                : args.getBoolean(KEY_ONLY_FREE);
        binding.onlyFree.switchIcon.setImageResource(R.drawable.ic_calendar_check);
        binding.onlyFree.switchTitle.setText(R.string.doctors_only_free);
        binding.onlyFree.switchSub.setText(R.string.doctors_only_free_sub);
        binding.onlyFree.switchToggle.setChecked(onlyFree);
        binding.onlyFree.switchRow.setOnClickListener(v -> {
            onlyFree = !onlyFree;
            binding.onlyFree.switchToggle.setChecked(onlyFree);
        });

        binding.btnReset.setOnClickListener(v -> {
            binding.sortGroup.check(R.id.sort_soonest);
            onlyFree = false;
            binding.onlyFree.switchToggle.setChecked(false);
        });
        binding.btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(KEY_SORT, sortOf(binding.sortGroup.getCheckedRadioButtonId()));
            result.putBoolean(KEY_ONLY_FREE, onlyFree);
            getParentFragmentManager().setFragmentResult(RESULT, result);
            dismiss();
        });
        return binding.getRoot();
    }

    private static int sortButton(String sort) {
        if (SORT_RATING.equals(sort)) {
            return R.id.sort_rating;
        }
        if (SORT_NAME.equals(sort)) {
            return R.id.sort_name;
        }
        return R.id.sort_soonest;
    }

    private static String sortOf(int buttonId) {
        if (buttonId == R.id.sort_rating) {
            return SORT_RATING;
        }
        if (buttonId == R.id.sort_name) {
            return SORT_NAME;
        }
        return SORT_SOONEST;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ONLY_FREE, onlyFree);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
