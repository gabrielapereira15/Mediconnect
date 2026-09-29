package com.example.mediconnect_android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.OTPClient;
import com.example.mediconnect_android.client.OTPClientImpl;
import com.example.mediconnect_android.client.PatientClient;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.client.response.AuthResult;
import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
import com.example.mediconnect_android.model.Patient;
import com.example.mediconnect_android.util.ActivityUtils;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.SessionManager;

import java.util.Locale;

/**
 * The passcode screen (board P03).
 *
 * Six boxes, one field. The screen this replaces had six separate EditTexts
 * wired together by five text watchers: pasting a code put all six digits
 * in the first box, an SMS autofill could not work at all, and backspace on
 * an empty box went nowhere. The boxes are drawn from one input, so paste
 * and autofill both land whole, and the code verifies itself on the sixth
 * digit rather than waiting for a button nobody should have to press.
 */
public class OTPActivity extends AppCompatActivity {

    private static final int DIGITS = 6;

    /** How long before the clinic will send another passcode. */
    private static final long RESEND_AFTER_MS = 30_000L;

    private ActivityOtpactivityBinding binding;
    private final OTPClient otpClient = new OTPClientImpl();

    private final TextView[] cells = new TextView[DIGITS];
    private CountDownTimer resendTimer;

    private String email;
    private boolean verifying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = getIntent().getStringExtra(SignInActivity.EXTRA_EMAIL);
        binding.otpInstructions.setText(getString(R.string.passcode_body, email));

        buildCells();
        watchInput();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnDifferentEmail.setOnClickListener(v -> finish());
        binding.verifyButton.setOnClickListener(v -> verify());
        binding.resendOtpText.setOnClickListener(v -> resend());

        startResendCountdown();
        focusInput();
    }

    // ---- the boxes ---------------------------------------------------------

    private void buildCells() {
        binding.otpCells.removeAllViews();
        int gap = getResources().getDimensionPixelSize(R.dimen.space_sm);

        for (int i = 0; i < DIGITS; i++) {
            TextView cell = new TextView(this);
            cell.setBackgroundResource(R.drawable.otp_cell);
            cell.setGravity(Gravity.CENTER);
            cell.setTextAppearance(R.style.TextAppearance_Mediconnect_Headline);
            cell.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface));
            cell.setMinHeight(getResources().getDimensionPixelSize(R.dimen.otp_cell_height));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginEnd(i == DIGITS - 1 ? 0 : gap);

            binding.otpCells.addView(cell, params);
            cells[i] = cell;
        }
        renderCells("");
    }

    private void renderCells(String code) {
        for (int i = 0; i < DIGITS; i++) {
            boolean filled = i < code.length();
            cells[i].setText(filled ? String.valueOf(code.charAt(i)) : "");
            // Activated marks where the next digit lands; selected marks the
            // ones already in. Neither relies on colour alone, because the
            // digit itself is the other half of the signal.
            cells[i].setActivated(i == code.length());
            cells[i].setSelected(filled);
        }
        binding.otpInput.setContentDescription(
                getString(R.string.cd_passcode, code.length()));
    }

    private void watchInput() {
        binding.otpInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String code = digitsOnly(s.toString());
                if (!code.equals(s.toString())) {
                    // A paste can carry spaces or dashes; keep the digits.
                    binding.otpInput.setText(code);
                    binding.otpInput.setSelection(code.length());
                    return;
                }

                renderCells(code);
                binding.verifyButton.setEnabled(code.length() == DIGITS);

                if (code.length() == DIGITS) {
                    verify();
                }
            }
        });
    }

    private String digitsOnly(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c) && out.length() < DIGITS) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void focusInput() {
        binding.otpInput.requestFocus();
        InputMethodManager imm = ContextCompat.getSystemService(this, InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(binding.otpInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ---- verifying ---------------------------------------------------------

    private void verify() {
        String code = binding.otpInput.getText() == null
                ? ""
                : binding.otpInput.getText().toString();

        // Auto-verify fires from the watcher, and the button can fire too;
        // without this a fast typist could send the same code twice.
        if (verifying || code.length() != DIGITS) {
            return;
        }
        verifying = true;
        binding.verifyButton.setEnabled(false);

        Background.run(
                () -> otpClient.verifyOTP(email, code),
                result -> {
                    verifying = false;
                    binding.verifyButton.setEnabled(true);
                    if (result.isVerified()) {
                        loginUser(result);
                        return;
                    }
                    // A wrong code leaves the boxes empty rather than making
                    // the patient clear six of them by hand.
                    binding.otpInput.setText("");
                    DialogUtils.showMessageDialog(this, getString(R.string.otp_invalid));
                },
                error -> {
                    verifying = false;
                    binding.verifyButton.setEnabled(true);
                    DialogUtils.showMessageDialog(this, getString(R.string.error_no_server));
                });
    }

    // ---- resending ----------------------------------------------------------

    /**
     * Resend is closed for half a minute.
     *
     * A patient who taps it three times in five seconds gets three
     * passcodes, only the last of which works — which looks like the app
     * rejecting the code they were just sent.
     */
    private void startResendCountdown() {
        binding.resendOtpText.setEnabled(false);

        resendTimer = new CountDownTimer(RESEND_AFTER_MS, 1000L) {
            @Override
            public void onTick(long remainingMs) {
                long seconds = remainingMs / 1000L;
                binding.resendOtpText.setText(getString(R.string.passcode_resend_in,
                        String.format(Locale.ENGLISH, "0:%02d", seconds)));
            }

            @Override
            public void onFinish() {
                binding.resendOtpText.setEnabled(true);
                binding.resendOtpText.setText(R.string.passcode_resend);
            }
        }.start();
    }

    private void resend() {
        binding.resendOtpText.setEnabled(false);
        Background.run(
                () -> otpClient.sendOTP(email, "patient"),
                sent -> {
                    DialogUtils.showMessageDialog(this, Boolean.TRUE.equals(sent)
                            ? getString(R.string.otp_resent)
                            : getString(R.string.error_no_server));
                    startResendCountdown();
                },
                error -> {
                    DialogUtils.showMessageDialog(this, getString(R.string.error_no_server));
                    startResendCountdown();
                });
    }

    // ---- what happens after a good passcode ---------------------------------

    private void loginUser(AuthResult result) {
        // Store the token first: every request after this point carries it.
        new SessionManager(this).createLoginSession(
                result.getEmail() == null ? email : result.getEmail(),
                result.getToken(),
                result.getExpiresInSeconds());

        // A brand new account has no profile yet, so skip the lookup entirely.
        if (result.isNewPatient()) {
            goToProfileForm();
            return;
        }

        // Otherwise cache the profile before deciding where to land.
        Background.run(this::isRegisteredPatient, registered -> {
            if (registered) {
                ActivityUtils.startActivity(this, MainActivity.class);
                finish();
            } else {
                goToProfileForm();
            }
        }, error -> goToProfileForm());
    }

    /** Sends a patient with no saved profile to the form to fill one in. */
    private void goToProfileForm() {
        saveToSharedPreferences(email);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("target_fragment", "EditProfileFragment");
        startActivity(intent);
        finish();
    }

    private void saveToSharedPreferences(String email) {
        getSharedPreferences("UserProfile", MODE_PRIVATE)
                .edit()
                .putString("email", email)
                .apply();
    }

    /**
     * Stores everything the signed-in patient's screens need.
     *
     * Takes the patient rather than eight positional strings, because the
     * call site already has one and adding a field previously meant editing
     * both ends and hoping the order still lined up.
     */
    private void saveCompletedataToSharedPreferences(Patient patient, String email) {
        SharedPreferences sharedPreferences =
                getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // The id is what the FHIR endpoints address this patient by, so the
        // health summary export needs it.
        editor.putString("patient_id", patient.getid());
        editor.putString("first_name", patient.getfirstName());
        editor.putString("last_name", patient.getlastName());
        editor.putString("email", email);
        editor.putString("phone_number", patient.getphoneNumber());
        editor.putString("clinic_code", patient.getclinicCode());
        editor.putString("address", patient.getaddress());
        editor.putString("dob", patient.getbirthdate());
        editor.putString("gender", patient.getgender());
        editor.putString("health_card_number", patient.getHealthCardNumber());
        editor.putString("health_card_province", patient.getHealthCardProvince());

        editor.apply();
    }

    private boolean isRegisteredPatient() {
        PatientClient patientClient = new PatientClientImpl();
        Patient patient = patientClient.getPatient(email);
        if (patient == null) {
            return false;
        }
        saveCompletedataToSharedPreferences(patient, email);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
