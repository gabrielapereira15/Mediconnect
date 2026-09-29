package com.example.mediconnect_android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.client.OTPClient;
import com.example.mediconnect_android.client.OTPClientImpl;
import com.example.mediconnect_android.client.PatientClient;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
import com.example.mediconnect_android.model.Patient;
import com.example.mediconnect_android.util.ActivityUtils;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.client.response.AuthResult;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.SessionManager;

public class OTPActivity extends AppCompatActivity {

    ActivityOtpactivityBinding binding;
    String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void init() {
        setupOTPFieldListeners();

        OTPClient otpClient = new OTPClientImpl();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
        }

        binding.verifyButton.setOnClickListener(v -> {
            // Check if the OTP fields are filled
            if (!areOTPFieldsFilled()) {
                return;
            }

            String otp = getOTP();
            binding.verifyButton.setEnabled(false);

            // Verifying hits the network, so it must not run on the UI thread.
            Background.run(
                    () -> otpClient.verifyOTP(email, otp),
                    result -> {
                        binding.verifyButton.setEnabled(true);
                        if (result.isVerified()) {
                            loginUser(result);
                        } else {
                            DialogUtils.showMessageDialog(this,
                                    getString(R.string.otp_invalid));
                        }
                    },
                    error -> {
                        binding.verifyButton.setEnabled(true);
                        DialogUtils.showMessageDialog(this, getString(R.string.error_no_server));
                    });
        });

        binding.resendOtpText.setOnClickListener(v -> Background.run(
                () -> otpClient.sendOTP(email, "patient"),
                sent -> DialogUtils.showMessageDialog(this, sent
                        ? getString(R.string.otp_resent)
                        : getString(R.string.error_no_server))));
    }

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
        Intent intent = new Intent(OTPActivity.this, MainActivity.class);
        intent.putExtra("target_fragment", "EditProfileFragment");
        startActivity(intent);
        finish();
    }

    private void saveToSharedPreferences(String email) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("UserProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.apply();
    }

    /**
     * Stores everything the signed-in patient's screens need.
     *
     * Takes the patient rather than eight positional strings, because the
     * call site already has one and adding a field previously meant editing
     * both ends and hoping the order still lined up.
     */
    private void saveCompletedataToSharedPreferences(Patient patient, String email) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
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
        String patientEmail = email;
        Patient patient = patientClient.getPatient(patientEmail);
        if (patient == null) {
            return false;
        }
        saveCompletedataToSharedPreferences(patient, patientEmail);
        return true;
    }


    private String getOTP() {
        EditText otpDigit1 = binding.otpDigit1;
        EditText otpDigit2 = binding.otpDigit2;
        EditText otpDigit3 = binding.otpDigit3;
        EditText otpDigit4 = binding.otpDigit4;
        EditText otpDigit5 = binding.otpDigit5;
        EditText otpDigit6 = binding.otpDigit6;

        String otp = otpDigit1.getText().toString() +
                otpDigit2.getText().toString() +
                otpDigit3.getText().toString() +
                otpDigit4.getText().toString() +
                otpDigit5.getText().toString() +
                otpDigit6.getText().toString();

        return otp;
    }

    private boolean areOTPFieldsFilled() {
        // Get references to the OTP fields
        EditText otpDigit1 = binding.otpDigit1;
        EditText otpDigit2 = binding.otpDigit2;
        EditText otpDigit3 = binding.otpDigit3;
        EditText otpDigit4 = binding.otpDigit4;
        EditText otpDigit5 = binding.otpDigit5;
        EditText otpDigit6 = binding.otpDigit6;

        // Reset error messages
        otpDigit1.setError(null);
        otpDigit2.setError(null);
        otpDigit3.setError(null);
        otpDigit4.setError(null);
        otpDigit5.setError(null);
        otpDigit6.setError(null);

        // Check if any OTP field is empty
        boolean isValid = true;

        if (otpDigit1.getText().toString().trim().isEmpty()) {
            otpDigit1.setError("This field is required");
            isValid = false;
        }

        if (otpDigit2.getText().toString().trim().isEmpty()) {
            otpDigit2.setError("This field is required");
            isValid = false;
        }

        if (otpDigit3.getText().toString().trim().isEmpty()) {
            otpDigit3.setError("This field is required");
            isValid = false;
        }

        if (otpDigit4.getText().toString().trim().isEmpty()) {
            otpDigit4.setError("This field is required");
            isValid = false;
        }

        if (otpDigit5.getText().toString().trim().isEmpty()) {
            otpDigit5.setError("This field is required");
            isValid = false;
        }

        if (otpDigit6.getText().toString().trim().isEmpty()) {
            otpDigit6.setError("This field is required");
            isValid = false;
        }

        // Return whether all fields are filled
        return isValid;
    }

    private void setupOTPFieldListeners() {
        binding.otpDigit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    binding.otpDigit2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.otpDigit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    binding.otpDigit3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.otpDigit3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    binding.otpDigit4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.otpDigit4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    binding.otpDigit5.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.otpDigit5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    binding.otpDigit6.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

}