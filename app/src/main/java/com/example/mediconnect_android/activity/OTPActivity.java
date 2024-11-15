package com.example.mediconnect_android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.view.KeyEvent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
import com.example.mediconnect_android.databinding.ActivityWelcomeBinding;

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
                Intent intent = new Intent(OTPActivity.this, MainActivity.class);
                startActivity(intent);
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
        binding.otpDigit1.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.otpDigit2.requestFocus();
                return true;
            }
            return false;
        });

        binding.otpDigit2.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.otpDigit3.requestFocus();
                return true;
            }
            return false;
        });

        binding.otpDigit3.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.otpDigit4.requestFocus();
                return true;
            }
            return false;
        });

        binding.otpDigit4.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.otpDigit5.requestFocus();
                return true;
            }
            return false;
        });

        binding.otpDigit5.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.otpDigit6.requestFocus();
                return true;
            }
            return false;
        });

        binding.otpDigit6.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                return true;
            }
            return false;
        });
    }

}