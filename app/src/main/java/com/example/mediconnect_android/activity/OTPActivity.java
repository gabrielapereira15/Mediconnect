package com.example.mediconnect_android.activity;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.OTPClient;
import com.example.mediconnect_android.client.OTPClientImpl;
import com.example.mediconnect_android.client.PatientClient;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
import com.example.mediconnect_android.fragment.EditProfileFragment;
import com.example.mediconnect_android.util.ActivityUtils;
import com.example.mediconnect_android.util.DialogUtils;
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
            if (areOTPFieldsFilled()) {
                String otp = getOTP();
                boolean isOtpVerified = otpClient.verifyOTP(email, otp);
                if (isOtpVerified) {
                    loginUser(email);
                } else {
                    DialogUtils.showMessageDialog(this, "OTP Error! The OTP inserted is invalid.");
                }
            }
        });
        binding.resendOtpText.setOnClickListener(v -> {
            boolean isOtpSent = otpClient.sendOTP(email, "patient");
            if (isOtpSent) {
                DialogUtils.showMessageDialog(this, "OTP re-sent successfully!");
            }
        });
    }

    private void loginUser(String email) {
        if (isRegisteredPatient()) {
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.createLoginSession(email);
            // Navigate to the main activity
            ActivityUtils.startActivity(this, MainActivity.class);
            finish();
        } else {
            // Navigate to the registration activity
            saveToSharedPreferences(email);
            Intent intent = new Intent(OTPActivity.this, MainActivity.class);
            intent.putExtra("target_fragment", "EditProfileFragment");
            startActivity(intent);
            finish();
        }
    }

    private void saveToSharedPreferences(String email) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("UserProfile", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.apply();
    }

    private boolean isRegisteredPatient() {
        PatientClient patientClient = new PatientClientImpl();
        String patientEmail = email;
        if (patientClient.getPatient(patientEmail) != null) {
            return true;
        } else {
            return false;
        }
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