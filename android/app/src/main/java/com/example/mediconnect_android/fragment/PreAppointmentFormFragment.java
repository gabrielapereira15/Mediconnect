package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.PreVisitFormClient;
import com.example.mediconnect_android.client.PreVisitFormClientImpl;
import com.example.mediconnect_android.databinding.FragmentPreAppointmentFormBinding;
import com.example.mediconnect_android.databinding.ViewCheckRowBinding;
import com.example.mediconnect_android.databinding.ViewYesNoQuestionBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.PreVisitForm;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FormVisitLine;
import com.example.mediconnect_android.util.KeyboardUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The pre-appointment form (board P10).
 *
 * Four questions, one at a time. The form this replaces put every question
 * on one scroll, insisted on three radio buttons, and then showed a dialog
 * saying it had been submitted — which was true of nothing, because nothing
 * was sent anywhere and no doctor ever read one.
 *
 * Answers are kept as a draft on the device as they are typed, so closing
 * the form in a waiting room and coming back to it does not start again.
 */
public class PreAppointmentFormFragment extends Fragment {

    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private static final String STATE_STEP = "step";

    /** Where drafts live, keyed by the visit they belong to. */
    private static final String DRAFTS = "PreVisitFormDrafts";

    /** Reported to whatever opened the form, so it can stop saying "pending". */
    public static final String RESULT_SENT = "preVisitFormSent";
    public static final String RESULT_SUBMITTED_AT = "submittedAt";

    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String UNSURE = "UNSURE";

    /** The symptom code that cancels the others. */
    private static final String NONE = "none";

    private static final int STEPS = 4;
    private static final int STEP_REASON = 0;
    private static final int STEP_SYMPTOMS = 1;
    private static final int STEP_HISTORY = 2;
    private static final int STEP_NOTES = 3;

    private FragmentPreAppointmentFormBinding binding;
    private final PreVisitFormClient formClient = new PreVisitFormClientImpl();

    private String appointmentId;

    private int step = STEP_REASON;
    private final PreVisitForm answers = new PreVisitForm();
    private final Set<String> symptoms = new LinkedHashSet<>();

    /** The three history questions, in the order they are asked. */
    private ViewYesNoQuestionBinding surgery;
    private ViewYesNoQuestionBinding smoking;
    private ViewYesNoQuestionBinding alcohol;

    public PreAppointmentFormFragment() {
    }

    /**
     * Opens the form for the visit it belongs to, carrying the doctor and
     * the time so the top of the form can say which visit that is.
     */
    public static PreAppointmentFormFragment of(Appointment appointment) {
        PreAppointmentFormFragment fragment = of(appointment.getId());
        FormVisitLine.put(fragment.requireArguments(), appointment);
        return fragment;
    }

    /**
     * Opens the form knowing only the visit's id, as a message does. The
     * form then looks the visit up itself to name it.
     */
    public static PreAppointmentFormFragment of(String appointmentId) {
        PreAppointmentFormFragment fragment = new PreAppointmentFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            appointmentId = args.getString(ARG_APPOINTMENT_ID);
        }
        if (savedInstanceState != null) {
            step = savedInstanceState.getInt(STATE_STEP, STEP_REASON);
        }
        restoreDraft();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPreAppointmentFormBinding.inflate(inflater, container, false);

        FormVisitLine.bind(binding.visitLine, getArguments(), appointmentId);
        binding.progress.setMax(STEPS);
        binding.textAnswer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                captureText();
                saveDraft();
            }
        });

        binding.btnBack.setOnClickListener(v -> goBack());
        binding.btnNext.setOnClickListener(v -> goNext());

        showStep();
        loadSentAnswers();
        return binding.getRoot();
    }

    /**
     * Fills the form in with what was already sent.
     *
     * Reopening a sent form showed four empty questions, and sending that
     * replaced the patient's real answers with nothing. A local draft wins,
     * because it is newer than anything the server has.
     */
    private void loadSentAnswers() {
        if (appointmentId == null || hasAnswers()) {
            return;
        }
        Background.run(
                () -> formClient.get(appointmentId),
                sent -> {
                    if (binding == null || sent == null) {
                        return;
                    }
                    answers.setReason(sent.getReason());
                    answers.setHadSurgery(sent.getHadSurgery());
                    answers.setSmokes(sent.getSmokes());
                    answers.setDrinksAlcohol(sent.getDrinksAlcohol());
                    answers.setNotes(sent.getNotes());
                    symptoms.clear();
                    symptoms.addAll(sent.getSymptoms());
                    showStep();
                },
                error -> { /* an unreachable server leaves the form empty */ });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.form_title);
    }

    // ---- the steps --------------------------------------------------------

    private void showStep() {
        binding.progress.setProgress(step + 1);
        binding.stepLabel.setText(getString(R.string.form_step,
                step + 1, STEPS, getString(stepName())));
        binding.btnBack.setVisibility(step == STEP_REASON ? View.GONE : View.VISIBLE);
        binding.btnNext.setText(step == STEP_NOTES ? R.string.form_send : R.string.form_next);

        binding.answers.removeAllViews();
        binding.textAnswerLayout.setVisibility(View.GONE);

        switch (step) {
            case STEP_SYMPTOMS:
                bindQuestion(R.string.form_q_symptoms, R.string.form_q_symptoms_help);
                buildSymptoms();
                break;
            case STEP_HISTORY:
                bindQuestion(R.string.form_q_history, R.string.form_q_history_help);
                buildHistory();
                break;
            case STEP_NOTES:
                bindQuestion(R.string.form_q_notes, R.string.form_q_notes_help);
                bindTextAnswer(answers.getNotes(), R.string.form_q_notes_hint);
                break;
            case STEP_REASON:
            default:
                bindQuestion(R.string.form_q_reason, R.string.form_q_reason_help);
                bindTextAnswer(answers.getReason(), R.string.form_q_reason_hint);
                break;
        }
    }

    private int stepName() {
        switch (step) {
            case STEP_SYMPTOMS:
                return R.string.form_step_symptoms;
            case STEP_HISTORY:
                return R.string.form_step_history;
            case STEP_NOTES:
                return R.string.form_step_notes;
            case STEP_REASON:
            default:
                return R.string.form_step_reason;
        }
    }

    private void bindQuestion(int question, int help) {
        binding.question.setText(question);
        binding.questionHelp.setText(help);
    }

    private void bindTextAnswer(String value, int hint) {
        binding.textAnswerLayout.setVisibility(View.VISIBLE);
        binding.textAnswer.setHint(hint);
        binding.textAnswer.setError(null);
        // Set before the watcher can mistake it for typing.
        binding.textAnswer.setText(value == null ? "" : value);
        binding.textAnswer.setSelection(binding.textAnswer.getText() == null
                ? 0
                : binding.textAnswer.getText().length());
    }

    private void buildSymptoms() {
        String[] codes = getResources().getStringArray(R.array.form_symptom_codes);
        String[] labels = getResources().getStringArray(R.array.form_symptom_labels);

        for (int i = 0; i < codes.length; i++) {
            String code = codes[i];
            ViewCheckRowBinding row =
                    ViewCheckRowBinding.inflate(getLayoutInflater(), binding.answers, false);
            row.checkLabel.setText(labels[i]);
            row.checkBox.setChecked(symptoms.contains(code));
            row.checkCard.setOnClickListener(v -> toggleSymptom(code));
            binding.answers.addView(row.getRoot());
        }
    }

    /**
     * "None of these" is the answer, not one of them. Ticking it clears the
     * rest; ticking anything else clears it.
     */
    private void toggleSymptom(String code) {
        if (symptoms.contains(code)) {
            symptoms.remove(code);
        } else if (NONE.equals(code)) {
            symptoms.clear();
            symptoms.add(NONE);
        } else {
            symptoms.remove(NONE);
            symptoms.add(code);
        }
        saveDraft();
        binding.answers.removeAllViews();
        buildSymptoms();
    }

    private void buildHistory() {
        surgery = addHistoryQuestion(R.string.form_q_surgery, answers.getHadSurgery(),
                answers::setHadSurgery);
        smoking = addHistoryQuestion(R.string.form_q_smoke, answers.getSmokes(),
                answers::setSmokes);
        alcohol = addHistoryQuestion(R.string.form_q_alcohol, answers.getDrinksAlcohol(),
                answers::setDrinksAlcohol);
    }

    private ViewYesNoQuestionBinding addHistoryQuestion(int label, String current,
                                                        java.util.function.Consumer<String> onAnswer) {
        ViewYesNoQuestionBinding question =
                ViewYesNoQuestionBinding.inflate(getLayoutInflater(), binding.answers, false);
        question.questionLabel.setText(label);

        if (YES.equals(current)) {
            question.answerGroup.check(R.id.answer_yes);
        } else if (NO.equals(current)) {
            question.answerGroup.check(R.id.answer_no);
        } else if (UNSURE.equals(current)) {
            question.answerGroup.check(R.id.answer_unsure);
        }

        question.answerGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.answer_yes) {
                onAnswer.accept(YES);
            } else if (checkedId == R.id.answer_no) {
                onAnswer.accept(NO);
            } else {
                onAnswer.accept(UNSURE);
            }
            saveDraft();
        });

        binding.answers.addView(question.getRoot());
        return question;
    }

    // ---- moving between them ----------------------------------------------

    private void goBack() {
        captureText();
        if (step == STEP_REASON) {
            return;
        }
        step--;
        showStep();
    }

    private void goNext() {
        captureText();

        // The reason is the one answer the doctor cannot do without. The
        // rest of the form is worth having, not worth blocking on.
        if (step == STEP_REASON && isBlank(answers.getReason())) {
            binding.textAnswer.setError(getString(R.string.form_reason_required));
            binding.textAnswer.requestFocus();
            return;
        }

        if (step < STEP_NOTES) {
            step++;
            showStep();
            return;
        }
        submit();
    }

    private void captureText() {
        if (binding == null || binding.textAnswerLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        String typed = binding.textAnswer.getText() == null
                ? ""
                : binding.textAnswer.getText().toString();
        if (step == STEP_NOTES) {
            answers.setNotes(typed);
        } else if (step == STEP_REASON) {
            answers.setReason(typed);
        }
    }

    // ---- sending ----------------------------------------------------------

    private void submit() {
        if (appointmentId == null || appointmentId.isEmpty()) {
            // Without a visit the answers have nowhere to go, and the old
            // form's cheerful "submitted" dialog is exactly what this is
            // replacing.
            DialogUtils.showMessageDialog(getContext(), getString(R.string.form_no_appointment));
            return;
        }

        answers.setSymptoms(new ArrayList<>(symptoms));
        KeyboardUtils.hideKeyboard(binding.getRoot(), getContext());
        binding.btnNext.setEnabled(false);

        Background.run(
                () -> formClient.submit(appointmentId, answers),
                saved -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnNext.setEnabled(true);
                    if (saved == null) {
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.form_send_failed));
                        return;
                    }
                    clearDraft();
                    // The screen behind this one is still showing "Form
                    // pending" from the arguments it was opened with.
                    Bundle result = new Bundle();
                    result.putString(RESULT_SUBMITTED_AT, saved.getSubmittedAt());
                    getParentFragmentManager().setFragmentResult(RESULT_SENT, result);
                    DialogUtils.showMessageDialog(getContext(), getString(R.string.form_sent));
                    getParentFragmentManager().popBackStack();
                },
                error -> {
                    if (binding != null) {
                        binding.btnNext.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.form_send_failed));
                    }
                });
    }

    // ---- the draft ---------------------------------------------------------

    /**
     * Kept on the device rather than sent as it is typed.
     *
     * A half-answered form is not something a doctor should be reading, and
     * a patient interrupted in a waiting room should not have to start
     * again.
     */
    private void saveDraft() {
        if (appointmentId == null) {
            return;
        }
        answers.setSymptoms(new ArrayList<>(symptoms));
        drafts().edit().putString(appointmentId, new Gson().toJson(answers)).apply();
        // Only claimed once there is something to have saved: showing it
        // over an empty form is the kind of small untruth this screen is
        // replacing.
        if (binding != null) {
            binding.draftSaved.setVisibility(hasAnswers() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void restoreDraft() {
        if (appointmentId == null) {
            return;
        }
        String stored = drafts().getString(appointmentId, null);
        if (stored == null) {
            return;
        }
        PreVisitForm draft = new Gson().fromJson(stored, PreVisitForm.class);
        if (draft == null) {
            return;
        }
        answers.setReason(draft.getReason());
        answers.setHadSurgery(draft.getHadSurgery());
        answers.setSmokes(draft.getSmokes());
        answers.setDrinksAlcohol(draft.getDrinksAlcohol());
        answers.setNotes(draft.getNotes());
        symptoms.addAll(draft.getSymptoms());
    }

    private void clearDraft() {
        if (appointmentId != null) {
            drafts().edit().remove(appointmentId).apply();
        }
    }

    private SharedPreferences drafts() {
        return requireContext().getSharedPreferences(DRAFTS, Context.MODE_PRIVATE);
    }

    private boolean hasAnswers() {
        return !symptoms.isEmpty()
                || !isBlank(answers.getReason())
                || !isBlank(answers.getNotes())
                || answers.getHadSurgery() != null
                || answers.getSmokes() != null
                || answers.getDrinksAlcohol() != null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
