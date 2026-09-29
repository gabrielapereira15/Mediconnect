package com.example.mediconnect_android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.OTPClient;
import com.example.mediconnect_android.client.OTPClientImpl;
import com.example.mediconnect_android.databinding.ActivitySignInBinding;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;

/**
 * Sign in (board P02).
 *
 * An email and a passcode: there is no password to get wrong, so the whole
 * screen is one field and one decision. It used to share the welcome
 * screen, which made a form the first thing a new patient saw, and the
 * terms were a separate page of placeholder Latin to navigate back out of.
 */
public class SignInActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private ActivitySignInBinding binding;
    private final OTPClient otpClient = new OTPClientImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Coming back from the passcode screen to correct a typo.
        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email != null) {
            binding.email.setText(email);
        }

        bindTerms();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> send());
    }

    /**
     * The terms sentence, with its two links live.
     *
     * Tapping a link opens what it names; tapping the rest of the sentence
     * does nothing, so nobody accepts terms by trying to read them.
     */
    private void bindTerms() {
        String terms = getString(R.string.signin_terms_link);
        String privacy = getString(R.string.signin_privacy_link);
        String sentence = getString(R.string.signin_terms, terms, privacy);

        SpannableString spanned = new SpannableString(sentence);
        link(spanned, sentence, terms, R.string.terms_and_conditions_title,
                R.string.signin_terms_body);
        link(spanned, sentence, privacy, R.string.profile_privacy,
                R.string.signin_privacy_body);

        binding.termsText.setText(spanned);
        binding.termsText.setMovementMethod(LinkMovementMethod.getInstance());
        binding.termsRow.setOnClickListener(v -> binding.termsCheckbox.toggle());
    }

    private void link(SpannableString spanned, String sentence, String label,
                      @StringRes int titleRes, @StringRes int bodyRes) {
        int start = sentence.indexOf(label);
        if (start < 0) {
            return;
        }
        int end = start + label.length();

        spanned.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                new AlertDialog.Builder(SignInActivity.this)
                        .setTitle(titleRes)
                        .setMessage(bodyRes)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spanned.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(this, R.color.md_primary)),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ---- sending the passcode ---------------------------------------------

    private void send() {
        String email = binding.email.getText() == null
                ? ""
                : binding.email.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailLayout.setError(getString(R.string.signin_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.signin_email_invalid));
            return;
        }
        binding.emailLayout.setError(null);

        if (!binding.termsCheckbox.isChecked()) {
            // Beside the checkbox rather than in a dialog: the error belongs
            // where the thing it is about is.
            binding.termsCheckbox.setError(getString(R.string.signin_terms_required));
            DialogUtils.showMessageDialog(this, getString(R.string.signin_terms_required));
            return;
        }

        binding.btnSend.setEnabled(false);
        Background.run(
                () -> otpClient.sendOTP(email, "patient"),
                sent -> {
                    binding.btnSend.setEnabled(true);
                    if (!Boolean.TRUE.equals(sent)) {
                        DialogUtils.showMessageDialog(this, getString(R.string.error_no_server));
                        return;
                    }
                    Intent intent = new Intent(this, OTPActivity.class);
                    intent.putExtra(EXTRA_EMAIL, email);
                    startActivity(intent);
                },
                error -> {
                    binding.btnSend.setEnabled(true);
                    DialogUtils.showMessageDialog(this, getString(R.string.error_no_server));
                });
    }
}
