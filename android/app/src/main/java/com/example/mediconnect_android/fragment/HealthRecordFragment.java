package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.HealthEntryAdapter;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.DialogAddHealthEntryBinding;
import com.example.mediconnect_android.databinding.FragmentHealthRecordBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DateFields;
import com.example.mediconnect_android.util.DialogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The patient's allergies, medications and conditions.
 *
 * This is the same information a clinic asks for on a paper form at the
 * desk, kept once instead of rewritten every visit — and it is what the
 * exported patient summary is made of, so it is worth keeping current.
 */
public class HealthRecordFragment extends Fragment {

    private FragmentHealthRecordBinding binding;
    private final HealthClient healthClient = new HealthClientImpl();
    private final List<HealthEntry> entries = new ArrayList<>();
    private HealthEntryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        adapter = new HealthEntryAdapter(entries, this::confirmStop);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.stateView.setContentView(binding.recyclerView);

        binding.fabAdd.setOnClickListener(v -> showAddDialog());
        binding.swipeRefresh.setOnRefreshListener(this::load);

        load();
    }

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
                    entries.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    binding.stateView.showContentOrEmpty(entries.isEmpty(),
                            R.drawable.baseline_medical_information_24,
                            R.string.health_empty_title,
                            R.string.health_empty_body);
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    binding.stateView.showError(this::load);
                });
    }

    /**
     * One dialog for all three kinds, because the fields are the same and
     * three near-identical forms would be three places to keep in step.
     */
    private void showAddDialog() {
        DialogAddHealthEntryBinding dialogBinding =
                DialogAddHealthEntryBinding.inflate(getLayoutInflater());

        String[] labels = {
                getString(R.string.health_allergies),
                getString(R.string.health_medications),
                getString(R.string.health_conditions)};
        String[] types = {
                HealthEntry.TYPE_ALLERGY,
                HealthEntry.TYPE_MEDICATION,
                HealthEntry.TYPE_CONDITION};

        dialogBinding.spinnerType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        DateFields.asDateOfBirth(requireContext(), dialogBinding.tilOnset, dialogBinding.etOnset);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.health_add_title)
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

            String type = types[dialogBinding.spinnerType.getSelectedItemPosition()];
            String note = dialogBinding.etNote.getText().toString().trim();
            String onset = dialogBinding.etOnset.getText().toString().trim();

            dialog.dismiss();
            save(type, description, note, onset);
        });
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

    private String email() {
        return requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reached from the Profile tab, which otherwise leaves its own title
        // on the toolbar and makes this look like part of that screen.
        requireActivity().setTitle(R.string.health_record_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
