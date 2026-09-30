package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.PreVisitFormClient;
import com.example.mediconnect_android.client.PreVisitFormClientImpl;
import com.example.mediconnect_android.databinding.FragmentFormAnswersBinding;
import com.example.mediconnect_android.databinding.ViewDetailRowBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.PreVisitForm;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.FormVisitLine;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.WhenLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * A sent pre-appointment form, read back.
 *
 * "View" on a sent form used to reopen the four-step form, which asked to
 * be filled in again and, whenever the answers had not arrived yet, showed
 * four empty questions as though they had been lost. This shows what the
 * doctor will read, and leaves changing it to one deliberate button.
 */
public class FormAnswersFragment extends Fragment {

    private static final String ARG_APPOINTMENT_ID = "appointmentId";

    private FragmentFormAnswersBinding binding;
    private final PreVisitFormClient formClient = new PreVisitFormClientImpl();
    private String appointmentId;

    public static FormAnswersFragment of(String appointmentId) {
        FormAnswersFragment fragment = new FormAnswersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * The right screen for a visit's form: the answers once it has been
     * sent, the form itself while it has not. Either way the visit goes
     * with it, so the screen can say which one it is.
     */
    public static Fragment forVisit(Appointment appointment) {
        if (!appointment.isFormSubmitted()) {
            return PreAppointmentFormFragment.of(appointment);
        }
        FormAnswersFragment fragment = of(appointment.getId());
        FormVisitLine.put(fragment.requireArguments(), appointment);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        appointmentId = args == null ? null : args.getString(ARG_APPOINTMENT_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFormAnswersBinding.inflate(inflater, container, false);
        binding.stateView.setContentView(binding.content);
        FormVisitLine.bind(binding.visitLine, getArguments(), appointmentId);
        binding.btnEdit.setOnClickListener(v -> edit());
        // Loaded every time the view is made, so coming back from Edit shows
        // what was just sent rather than what was there before.
        load();
        return binding.getRoot();
    }

    /** The form again, still naming the visit this screen was opened for. */
    private void edit() {
        PreAppointmentFormFragment form = PreAppointmentFormFragment.of(appointmentId);
        FormVisitLine.copy(getArguments(), form.requireArguments());
        FragmentUtils.loadFragment(getParentFragmentManager(), R.id.flFragment, form);
    }

    private void load() {
        binding.stateView.showLoading();
        binding.btnEdit.setEnabled(false);
        Background.run(
                () -> formClient.get(appointmentId),
                sent -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnEdit.setEnabled(true);
                    if (sent == null) {
                        // The visit says it was sent but the clinic has no
                        // answers on file; filling it in is the only fix.
                        binding.stateView.showEmpty(R.drawable.ic_form,
                                R.string.form_answers_missing_title,
                                R.string.form_answers_missing_body,
                                R.string.visit_fill_in_form,
                                () -> binding.btnEdit.performClick());
                        return;
                    }
                    render(sent);
                    binding.stateView.showContent();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.stateView.showError(this::load);
                });
    }

    private void render(PreVisitForm sent) {
        binding.sentAt.setText(WhenLabel.parse(sent.getSubmittedAt())
                .map(at -> getString(R.string.form_answers_sent_at,
                        WhenLabel.whenWords(requireContext(), at)))
                .orElse(getString(R.string.form_sent)));

        binding.rows.removeAllViews();
        addRow(R.string.form_step_reason, sent.getReason());
        addRow(R.string.form_step_symptoms, symptomWords(sent.getSymptoms()));
        addRow(R.string.form_q_surgery, answerWords(sent.getHadSurgery()));
        addRow(R.string.form_q_smoke, answerWords(sent.getSmokes()));
        addRow(R.string.form_q_alcohol, answerWords(sent.getDrinksAlcohol()));
        addRow(R.string.form_step_notes, sent.getNotes());
    }

    private void addRow(@StringRes int label, @Nullable String value) {
        ViewDetailRowBinding row =
                ViewDetailRowBinding.inflate(getLayoutInflater(), binding.rows, false);
        row.detailLabel.setText(label);
        boolean answered = value != null && !value.trim().isEmpty();
        // Said in words rather than left blank: a blank line reads like
        // something failed to load.
        row.detailValue.setText(answered ? value.trim() : getString(R.string.form_answers_none));
        row.detailValue.setTextColor(requireContext().getColor(answered
                ? R.color.md_on_surface
                : R.color.md_on_surface_variant));
        binding.rows.addView(row.getRoot());
    }

    /** The codes sent back as the labels the patient ticked. */
    private String symptomWords(@Nullable List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        String[] known = getResources().getStringArray(R.array.form_symptom_codes);
        String[] labels = getResources().getStringArray(R.array.form_symptom_labels);

        List<String> words = new ArrayList<>();
        for (String code : codes) {
            String word = code;
            for (int i = 0; i < known.length; i++) {
                if (known[i].equals(code)) {
                    word = labels[i];
                    break;
                }
            }
            words.add(word);
        }
        return String.join(", ", words);
    }

    @Nullable
    private String answerWords(@Nullable String answer) {
        if (answer == null) {
            return null;
        }
        switch (answer) {
            case "YES":
                return getString(R.string.form_yes);
            case "NO":
                return getString(R.string.form_no);
            case "UNSURE":
                return getString(R.string.form_prefer_not);
            default:
                return answer;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.form_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
