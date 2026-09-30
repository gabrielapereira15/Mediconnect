package com.example.mediconnect_android.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.ReviewClient;
import com.example.mediconnect_android.client.ReviewClientImpl;
import com.example.mediconnect_android.databinding.SheetLeaveReviewBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.WhenLabel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.time.LocalDateTime;

/**
 * Leaving a review (board P11).
 *
 * A sheet over the list rather than a screen of its own: the review belongs
 * to the visit behind it, and a patient who changes their mind can swipe it
 * away instead of navigating back out of somewhere.
 *
 * Five stars with a word under them. A number alone makes a patient guess
 * whether four out of five is generous or damning, and the word is also
 * what a screen reader announces.
 */
public class LeaveReviewSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LeaveReviewSheet";

    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private static final String ARG_DOCTOR_NAME = "doctorName";
    private static final String ARG_STARTS_AT = "startsAt";
    private static final String STATE_SCORE = "score";

    private static final int MAX_SCORE = 5;

    private SheetLeaveReviewBinding binding;
    private final ReviewClient reviewClient = new ReviewClientImpl();

    private String appointmentId;
    private String doctorName;
    private String startsAt;
    private int score;

    /** Called back when a review lands, so the list can show it. */
    public interface OnReviewSent {
        void onReviewSent();
    }

    public static LeaveReviewSheet of(Appointment appointment) {
        LeaveReviewSheet sheet = new LeaveReviewSheet();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointment.getId());
        args.putString(ARG_STARTS_AT, appointment.getStartsAt());
        args.putString(ARG_DOCTOR_NAME, appointment.getDoctor() == null
                ? null
                : appointment.getDoctor().getName());
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        appointmentId = args.getString(ARG_APPOINTMENT_ID);
        doctorName = WhenLabel.doctorName(args.getString(ARG_DOCTOR_NAME));
        startsAt = args.getString(ARG_STARTS_AT);
        if (savedInstanceState != null) {
            score = savedInstanceState.getInt(STATE_SCORE, 0);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // The sheet draws its own rounded top, so Material's own background
        // would show as a second, squarer one behind it.
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetLeaveReviewBinding.inflate(inflater, container, false);

        binding.doctorInitials.setText(WhenLabel.initials(doctorName));
        binding.reviewQuestion.setText(getString(R.string.review_sheet_question, doctorName));
        binding.reviewSub.setText(getString(R.string.review_sheet_sub, visitDay()));

        buildStars();
        bindScore();
        binding.btnSendReview.setOnClickListener(v -> send());

        return binding.getRoot();
    }

    private String visitDay() {
        return WhenLabel.parse(startsAt)
                .map(LocalDateTime::toLocalDate)
                .map(day -> day.format(java.time.format.DateTimeFormatter
                        .ofPattern("EEE d MMM", java.util.Locale.ENGLISH)))
                .orElse("");
    }

    // ---- the stars --------------------------------------------------------

    private void buildStars() {
        binding.stars.removeAllViews();
        int size = getResources().getDimensionPixelSize(R.dimen.review_star_size);
        int gap = getResources().getDimensionPixelSize(R.dimen.space_sm);

        for (int i = 1; i <= MAX_SCORE; i++) {
            int value = i;
            ImageView star = new ImageView(requireContext());
            star.setContentDescription(getString(R.string.cd_star, value));
            star.setBackgroundResource(R.drawable.ripple_borderless);
            star.setOnClickListener(v -> {
                score = value;
                bindScore();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(i == MAX_SCORE ? 0 : gap);
            binding.stars.addView(star, params);
        }
    }

    private void bindScore() {
        String[] words = getResources().getStringArray(R.array.review_score_words);

        for (int i = 0; i < binding.stars.getChildCount(); i++) {
            ImageView star = (ImageView) binding.stars.getChildAt(i);
            boolean filled = i < score;
            star.setImageResource(filled ? R.drawable.ic_star : R.drawable.ic_star_outline);
            star.setImageTintList(ContextCompat.getColorStateList(requireContext(),
                    filled ? R.color.md_rating : R.color.md_outline));
        }

        binding.scoreWord.setText(score == 0 ? "" : words[score - 1]);
        binding.stars.setContentDescription(score == 0
                ? getString(R.string.review_pick_score)
                : getString(R.string.cd_star, score));
    }

    // ---- sending -----------------------------------------------------------

    private void send() {
        if (score == 0) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.review_pick_score));
            return;
        }

        String text = binding.reviewText.getText() == null
                ? ""
                : binding.reviewText.getText().toString().trim();
        double chosen = score;

        binding.btnSendReview.setEnabled(false);
        Background.run(
                () -> reviewClient.createReview(appointmentId, chosen, text),
                sent -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnSendReview.setEnabled(true);
                    if (!Boolean.TRUE.equals(sent)) {
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.review_send_failed));
                        return;
                    }
                    if (getParentFragment() instanceof OnReviewSent) {
                        ((OnReviewSent) getParentFragment()).onReviewSent();
                    }
                    dismiss();
                    DialogUtils.showMessageDialog(getContext(), getString(R.string.review_sent));
                },
                error -> {
                    if (binding != null) {
                        binding.btnSendReview.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server));
                    }
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SCORE, score);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
