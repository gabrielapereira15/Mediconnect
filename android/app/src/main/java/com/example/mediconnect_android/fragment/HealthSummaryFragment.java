package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.FragmentHealthSummaryBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The patient's health summary, in a form they can hand to someone else.
 *
 * The clinic's server can produce this as a pan-Canadian Patient Summary —
 * a FHIR document another Canadian system can read directly. This screen is
 * the human side of it: what the summary says, and a way to pass it on.
 *
 * What is shared is the readable summary rather than the raw FHIR document.
 * The structured version exists for systems, and is fetched by them from
 * the API; a patient sharing their record with a person wants something
 * that person can read.
 */
public class HealthSummaryFragment extends Fragment {

    private FragmentHealthSummaryBinding binding;
    private final HealthClient healthClient = new HealthClientImpl();

    /** Held so Share does not have to rebuild it. */
    private String summaryText = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHealthSummaryBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.tvGenerated.setText(getString(R.string.summary_generated,
                DateFormat.getDateInstance(DateFormat.LONG).format(new Date())));

        binding.btnShare.setEnabled(false);
        binding.btnShare.setOnClickListener(v -> share());

        load();
    }

    private void load() {
        binding.stateView.setContentView(binding.content);
        binding.stateView.showLoading();

        String email = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");

        Background.run(
                () -> healthClient.getEntries(email),
                entries -> {
                    if (binding == null) {
                        return;
                    }
                    render(entries);
                    binding.stateView.showContent();
                    binding.btnShare.setEnabled(true);
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.stateView.showError(this::load);
                });
    }

    private void render(List<HealthEntry> entries) {
        String name = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("first_name", "") + " "
                + requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("last_name", "");

        binding.tvName.setText(name.trim());

        binding.tvAllergies.setText(section(entries, HealthEntry.TYPE_ALLERGY));
        binding.tvMedications.setText(section(entries, HealthEntry.TYPE_MEDICATION));
        binding.tvConditions.setText(section(entries, HealthEntry.TYPE_CONDITION));

        summaryText = getString(R.string.summary_share_subject) + " — " + name.trim() + "\n\n"
                + getString(R.string.health_allergies) + ":\n"
                + section(entries, HealthEntry.TYPE_ALLERGY) + "\n\n"
                + getString(R.string.health_medications) + ":\n"
                + section(entries, HealthEntry.TYPE_MEDICATION) + "\n\n"
                + getString(R.string.health_conditions) + ":\n"
                + section(entries, HealthEntry.TYPE_CONDITION);
    }

    /**
     * One section of the summary.
     *
     * An entry that has been stopped is still listed, marked as such —
     * the same reason the record keeps it. "None recorded" is deliberately
     * not "none": the app has never asked anyone to confirm they have no
     * allergies, and a summary that claims otherwise would be wrong.
     */
    private String section(List<HealthEntry> entries, String type) {
        List<HealthEntry> matching = entries.stream()
                .filter(entry -> type.equals(entry.getType()))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return getString(R.string.health_none_recorded);
        }

        return matching.stream()
                .map(entry -> {
                    String line = "• " + entry.getDescription();
                    if (entry.getNote() != null && !entry.getNote().isEmpty()) {
                        line += " — " + entry.getNote();
                    }
                    if (!entry.isActive()) {
                        line += " (" + getString(R.string.health_no_longer_current) + ")";
                    }
                    return line;
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * Hands the summary to whatever the patient picks.
     *
     * Deliberately their action, through the system share sheet: this is
     * health information, and where it goes is a decision only they can
     * make.
     */
    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, summaryText);
        startActivity(Intent.createChooser(intent, getString(R.string.summary_share)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
