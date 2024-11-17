package com.example.mediconnect_android.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
import com.example.mediconnect_android.util.ActivityUtils;

public class OTPActivity extends AppCompatActivity {

    ActivityOtpactivityBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        setupOTPFieldListeners();

        binding.verifyButton.setOnClickListener(v -> {
            // Check if the OTP fields are filled
            if (areOTPFieldsFilled()) {
                // If filled, proceed to the next activity
                ActivityUtils.startActivity(OTPActivity.this, MainActivity.class);
            }
        });
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