package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.ApiException;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.databinding.FragmentHealthSummaryBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.model.Patient;
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    private static final String EXPORT_FILE_NAME = "mediconnect-patient-summary.json";

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
     * The system's "save as" screen, for Download.
     *
     * The patient picks where the file goes (Downloads by default), and the
     * app needs no storage permission to write the one file they chose.
     * Registered as a field so it is in place before the fragment starts,
     * which the result API requires.
     */
    private final ActivityResultLauncher<String> saveDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            this::downloadTo);

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
            download();
        }
    }

    /**
     * Saves the PS-CA document as a file on the phone.
     *
     * Download previously opened the share sheet, which is Export under
     * another name: it asked where to send the file rather than keeping a
     * copy. The picker comes first and the document is fetched once there
     * is somewhere to put it, so nothing is held in memory while the
     * patient decides.
     */
    private void download() {
        if (DemoMode.isActive()) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.summary_export_offline));
            return;
        }
        saveDocument.launch(EXPORT_FILE_NAME);
    }

    private void downloadTo(Uri destination) {
        if (destination == null || binding == null) {
            // They backed out of the picker; nothing to say about that.
            return;
        }
        Context context = requireContext().getApplicationContext();
        Background.run(
                () -> {
                    String document = summaryDocument();
                    try (OutputStream out = context.getContentResolver().openOutputStream(destination)) {
                        if (out == null) {
                            throw new IOException("Could not open the chosen file");
                        }
                        out.write(document.getBytes(StandardCharsets.UTF_8));
                    }
                    return destination;
                },
                saved -> {
                    if (binding == null) {
                        return;
                    }
                    Snackbar.make(binding.getRoot(), R.string.summary_downloaded,
                            Snackbar.LENGTH_LONG).show();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    DialogUtils.showMessageDialog(getContext(), getString(R.string.error_no_server));
                });
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
        if (DemoMode.isActive()) {
            // The document is built by the clinic's server, and the app is
            // showing bundled sample data because it cannot reach it.
            DialogUtils.showMessageDialog(getContext(), getString(R.string.summary_export_offline));
            return;
        }

        binding.btnExport.setEnabled(false);
        Background.run(
                () -> {
                    String document = summaryDocument();
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

    /**
     * The PS-CA document, asked for by this patient's id.
     *
     * The stored id can be out of date — the demo server reseeds with new
     * ids every time it restarts — and a stale one is refused as somebody
     * else's. So a refusal forgets it and asks once more with a fresh one,
     * rather than failing until the patient signs out and in again.
     */
    private String summaryDocument() {
        try {
            return healthClient.getSummaryDocument(patientId());
        } catch (ApiException e) {
            if (e.getStatus() != 403 && e.getStatus() != 404) {
                throw e;
            }
            requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    .edit().remove("patient_id").commit();
            return healthClient.getSummaryDocument(patientId());
        }
    }

    /**
     * The id the FHIR endpoints address this patient by.
     *
     * Saved at sign-in, but only by builds that knew to save it: anyone
     * signed in before that had no id stored, and Export told them to sign
     * in again. Asking the server by email costs one request and spares
     * them that. Runs off the main thread.
     */
    private String patientId() {
        SharedPreferences profile = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String stored = profile.getString("patient_id", "");
        if (!stored.isEmpty()) {
            return stored;
        }

        Patient patient = new PatientClientImpl().getPatient(profile.getString("email", ""));
        if (patient == null || patient.getid() == null || patient.getid().isEmpty()) {
            throw new IllegalStateException("No patient id for this account");
        }
        profile.edit().putString("patient_id", patient.getid()).apply();
        return patient.getid();
    }

    private File writeToCache(String document) throws IOException {
        File directory = new File(requireContext().getCacheDir(), "exports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the export folder");
        }
        // One fixed name, overwritten each time: a folder quietly filling up
        // with old copies of somebody's health record is not something to
        // leave behind.
        File file = new File(directory, EXPORT_FILE_NAME);
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
