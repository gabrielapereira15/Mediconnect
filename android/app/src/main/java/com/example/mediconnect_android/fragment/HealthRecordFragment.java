package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.DialogAddHealthEntryBinding;
import com.example.mediconnect_android.databinding.FragmentHealthRecordBinding;
import com.example.mediconnect_android.databinding.HealthEntryItemBinding;
import com.example.mediconnect_android.databinding.ViewHealthSectionBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DateFields;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The patient's allergies, medications and conditions (board P12).
 *
 * The same information a clinic asks for on a paper form at the desk, kept
 * once instead of rewritten every visit — and it is what the exported
 * patient summary is made of, so it is worth keeping current.
 *
 * Grouped by kind rather than listed flat. A flat list wrote the kind out
 * on every row and still made four entries look like one long run of them;
 * the headings carry it instead, and "Add" belongs to the section so the
 * dialog does not have to ask which kind a second time.
 */
public class HealthRecordFragment extends Fragment {

    private FragmentHealthRecordBinding binding;
    private final HealthClient healthClient = new HealthClientImpl();
    private final List<HealthEntry> entries = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.stateView.setContentView(binding.content);
        binding.swipeRefresh.setOnRefreshListener(this::load);

        // Both buttons lead to the same summary, which is the one place the
        // document is built; they differ in what it does when it gets there.
        binding.btnShare.setOnClickListener(v -> openSummary(HealthSummaryFragment.ACTION_SHARE));
        binding.btnDownload.setOnClickListener(
                v -> openSummary(HealthSummaryFragment.ACTION_DOWNLOAD));

        bindSectionHeading(binding.sectionAllergies, R.string.health_allergies,
                HealthEntry.TYPE_ALLERGY);
        bindSectionHeading(binding.sectionMedications, R.string.health_medications,
                HealthEntry.TYPE_MEDICATION);
        bindSectionHeading(binding.sectionConditions, R.string.health_conditions,
                HealthEntry.TYPE_CONDITION);

        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.health_record_title);
    }

    // ---- loading ----------------------------------------------------------

    private void load() {
        binding.stateView.showLoading();
        Background.run(
                () -> healthClient.getEntries(email()),
                loaded -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    entries.clear();
                    if (loaded != null) {
                        entries.addAll(loaded);
                    }
                    bindSections();
                    // The sections stay on screen when the record is empty:
                    // each one says what belongs in it, which is more use
                    // than one message standing in for all three.
                    binding.stateView.showContent();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    binding.stateView.showError(this::load);
                });
    }

    // ---- the sections -----------------------------------------------------

    private void bindSectionHeading(ViewHealthSectionBinding section, int titleRes, String type) {
        section.sectionTitle.setText(titleRes);
        section.sectionAdd.setContentDescription(
                getString(R.string.health_add_kind, getString(titleRes)));
        section.sectionAdd.setOnClickListener(v -> showAddDialog(type));
    }

    private void bindSections() {
        bindSection(binding.sectionAllergies, HealthEntry.TYPE_ALLERGY,
                R.string.health_section_empty_allergies);
        bindSection(binding.sectionMedications, HealthEntry.TYPE_MEDICATION,
                R.string.health_section_empty_medications);
        bindSection(binding.sectionConditions, HealthEntry.TYPE_CONDITION,
                R.string.health_section_empty_conditions);
    }

    private void bindSection(ViewHealthSectionBinding section, String type, int emptyRes) {
        List<HealthEntry> forType = entries.stream()
                .filter(entry -> type.equals(entry.getType()))
                .collect(Collectors.toList());

        section.sectionCount.setText(forType.isEmpty() ? "" : String.valueOf(forType.size()));
        section.sectionRows.removeAllViews();
        section.sectionCard.setVisibility(forType.isEmpty() ? View.GONE : View.VISIBLE);
        section.sectionEmpty.setVisibility(forType.isEmpty() ? View.VISIBLE : View.GONE);
        section.sectionEmpty.setText(emptyRes);

        for (int i = 0; i < forType.size(); i++) {
            if (i > 0) {
                section.sectionRows.addView(divider());
            }
            section.sectionRows.addView(row(section, forType.get(i), type));
        }
    }

    private View row(ViewHealthSectionBinding section, HealthEntry entry, String type) {
        HealthEntryItemBinding row = HealthEntryItemBinding.inflate(
                getLayoutInflater(), section.sectionRows, false);

        row.entryDescription.setText(entry.getDescription());
        row.entryIcon.setImageResource(iconFor(type));
        row.entryIcon.setBackgroundResource(tileFor(type));
        row.entryIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), tintFor(type)));

        String sub = subtitle(entry);
        row.entryNote.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
        row.entryNote.setText(sub);

        boolean active = entry.isActive();
        row.entryInactive.setVisibility(active ? View.GONE : View.VISIBLE);
        row.entryDescription.setAlpha(active ? 1f : 0.6f);
        row.entryChevron.setVisibility(active ? View.VISIBLE : View.GONE);
        row.entryRow.setContentDescription(getString(R.string.cd_health_entry,
                entry.getDescription(), sub.isEmpty() ? "" : sub));

        if (active) {
            row.entryRow.setOnClickListener(v -> confirmStop(entry));
        } else {
            row.entryRow.setOnClickListener(null);
            row.entryRow.setClickable(false);
        }
        return row.getRoot();
    }

    /** The note, and when it started, on one line the way a chart reads. */
    private String subtitle(HealthEntry entry) {
        String note = entry.getNote() == null ? "" : entry.getNote().trim();
        String onset = entry.getOnsetDate() == null ? "" : entry.getOnsetDate().trim();

        if (onset.isEmpty()) {
            return note;
        }
        String since = getString(R.string.health_since, onset);
        return note.isEmpty() ? since : getString(R.string.health_line, note, since);
    }

    private int iconFor(String type) {
        if (HealthEntry.TYPE_MEDICATION.equals(type)) {
            return R.drawable.ic_pill;
        }
        if (HealthEntry.TYPE_CONDITION.equals(type)) {
            return R.drawable.ic_pulse;
        }
        return R.drawable.ic_alert;
    }

    /** Allergies are tinted danger: the one entry a clinician must not miss. */
    private int tileFor(String type) {
        if (HealthEntry.TYPE_MEDICATION.equals(type)) {
            return R.drawable.tile_brand_soft;
        }
        if (HealthEntry.TYPE_CONDITION.equals(type)) {
            return R.drawable.tile_blue_soft;
        }
        return R.drawable.tile_danger_soft;
    }

    private int tintFor(String type) {
        if (HealthEntry.TYPE_MEDICATION.equals(type)) {
            return R.color.md_on_primary_container;
        }
        if (HealthEntry.TYPE_CONDITION.equals(type)) {
            return R.color.md_on_secondary_container;
        }
        return R.color.md_error;
    }

    private View divider() {
        View line = new View(requireContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.stroke_hairline)));
        line.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.md_outline_variant));
        return line;
    }

    // ---- adding and stopping ----------------------------------------------

    /**
     * One dialog for all three kinds, because the fields are the same and
     * three near-identical forms would be three places to keep in step. The
     * kind comes from the section that opened it rather than a spinner the
     * patient has already answered by tapping.
     */
    private void showAddDialog(String type) {
        DialogAddHealthEntryBinding dialogBinding =
                DialogAddHealthEntryBinding.inflate(getLayoutInflater());

        DateFields.asDateOfBirth(requireContext(), dialogBinding.tilOnset, dialogBinding.etOnset);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.health_add_kind, getString(titleFor(type))))
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.health_add_save, null)
                .create();

        dialog.show();

        // Wired after show() so a validation failure can keep the dialog
        // open; the default positive button always dismisses.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String description = dialogBinding.etDescription.getText().toString().trim();
            if (description.isEmpty()) {
                dialogBinding.tilDescription.setError(getString(R.string.health_add_required));
                return;
            }
            dialogBinding.tilDescription.setError(null);

            String note = dialogBinding.etNote.getText().toString().trim();
            String onset = dialogBinding.etOnset.getText().toString().trim();

            dialog.dismiss();
            save(type, description, note, onset);
        });
    }

    private int titleFor(String type) {
        if (HealthEntry.TYPE_MEDICATION.equals(type)) {
            return R.string.health_type_medication;
        }
        if (HealthEntry.TYPE_CONDITION.equals(type)) {
            return R.string.health_type_condition;
        }
        return R.string.health_type_allergy;
    }

    private void save(String type, String description, String note, String onset) {
        Background.run(
                () -> healthClient.addEntry(email(), type, description, note, onset),
                saved -> load(),
                error -> DialogUtils.showMessageDialog(getContext(),
                        getString(R.string.error_no_server)));
    }

    /**
     * Stopping is not deleting.
     *
     * A reaction someone had ten years ago still matters to whoever treats
     * them next, so the entry stays on the record marked no longer current
     * — and the wording says that rather than implying it disappears.
     */
    private void confirmStop(HealthEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.health_stop_title, entry.getDescription()))
                .setMessage(R.string.health_stop_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.health_stop_confirm, (dialog, which) -> Background.run(
                        () -> healthClient.stopEntry(email(), entry.getId()),
                        stopped -> load(),
                        error -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server))))
                .show();
    }

    private void openSummary(String action) {
        FragmentUtils.loadFragment(getParentFragmentManager(), R.id.flFragment,
                HealthSummaryFragment.of(action));
    }

    private String email() {
        return requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
