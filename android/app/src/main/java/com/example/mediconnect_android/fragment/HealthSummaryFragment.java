package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.FragmentHealthSummaryBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * Two things can leave this screen. Share sends the readable summary as
 * text, which is what a person wants. Export sends the PS-CA document
 * itself — a FHIR Bundle another Canadian system can ingest — because that
 * is the whole point of implementing the standard, and a screenshot of this
 * page would not give a clinic anything it could use.
 */
public class HealthSummaryFragment extends Fragment {

    /** What the screen should do as soon as it has the summary. */
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_DOWNLOAD = "download";

    private static final String ARG_ACTION = "action";

    /**
     * Opens the summary and carries out one action once it has loaded.
     *
     * The health record offers Share and Download as two buttons, but the
     * document is built in one place; this is how those buttons reach it
     * without a second implementation.
     */
    public static HealthSummaryFragment of(String action) {
        HealthSummaryFragment fragment = new HealthSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION, action);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentHealthSummaryBinding binding;

    /** Runs once, when the summary first arrives. */
    private String pendingAction;
    private final HealthClient healthClient = new HealthClientImpl();

    /** Held so Share does not have to rebuild it. */
    private String summaryText = "";

    /**
     * The one-off action is read here, not with the view.
     *
     * Reading it in onCreateView meant a rotation or a switch to dark mode
     * exported the document again, or reopened the share sheet.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        pendingAction = savedInstanceState != null
                ? null
                : (args == null ? null : args.getString(ARG_ACTION));
    }

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

        binding.btnExport.setEnabled(false);
        binding.btnExport.setOnClickListener(v -> exportDocument());

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
                    // The document comes from the server, so there is
                    // nothing to export while the app is running on its
                    // bundled demo data.
                    binding.btnExport.setEnabled(!DemoMode.isActive());
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

        runPendingAction();
    }

    /**
     * Carries out whatever the health record's button asked for, once.
     *
     * Only after the summary exists: sharing an empty string because the
     * request had not landed yet is worse than a moment's wait.
     */
    private void runPendingAction() {
        if (pendingAction == null) {
            return;
        }
        String action = pendingAction;
        pendingAction = null;

        if (ACTION_SHARE.equals(action)) {
            share();
        } else if (ACTION_DOWNLOAD.equals(action)) {
            exportDocument();
        }
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

    /**
     * Fetches the PS-CA document and hands it over as a .json file.
     *
     * Written to the app's own cache and shared through a FileProvider, so
     * the receiving app is granted read access to this one file and nothing
     * else. The patient picks the destination; health data does not leave
     * the device on anyone else's decision.
     */
    private void exportDocument() {
        String patientId = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("patient_id", "");

        if (patientId.isEmpty()) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.summary_export_unavailable));
            return;
        }

        binding.btnExport.setEnabled(false);
        Background.run(
                () -> {
                    String document = healthClient.getSummaryDocument(patientId);
                    return writeToCache(document);
                },
                file -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnExport.setEnabled(true);
                    shareFile(file);
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnExport.setEnabled(true);
                    DialogUtils.showMessageDialog(getContext(), getString(R.string.error_no_server));
                });
    }

    private File writeToCache(String document) throws IOException {
        File directory = new File(requireContext().getCacheDir(), "exports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the export folder");
        }
        // One fixed name, overwritten each time: a folder quietly filling up
        // with old copies of somebody's health record is not something to
        // leave behind.
        File file = new File(directory, "mediconnect-patient-summary.json");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(document.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/fhir+json");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.summary_export)));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.summary_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
