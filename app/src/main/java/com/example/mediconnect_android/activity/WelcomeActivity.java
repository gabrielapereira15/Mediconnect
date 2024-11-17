package com.example.mediconnect_android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.databinding.ActivityWelcomeBinding;
import com.example.mediconnect_android.util.ActivityUtils;

public class WelcomeActivity extends AppCompatActivity {

    ActivityWelcomeBinding welcomeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        welcomeBinding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(welcomeBinding.getRoot());

        init();
    }

    private void init() {
        listeners();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TERMS_AGREED")) {
            boolean isTermsAgreed = intent.getBooleanExtra("TERMS_AGREED", false);
            welcomeBinding.checkboxTerms.setChecked(isTermsAgreed);
        }
    }

    private void listeners() {
        welcomeBinding.submitButton.setOnClickListener(v -> {
            // Check if the fields are filled correctly
            if (areFieldsFilled()) {
                // If fields are filled, proceed to OTPActivity
                ActivityUtils.startActivity(WelcomeActivity.this, OTPActivity.class);
            }
        });
        welcomeBinding.termsLink.setOnClickListener(v -> {
            // Handle terms and conditions button click
            ActivityUtils.startActivity(WelcomeActivity.this, TermsConditionsActivity.class);
        });
    }

    private boolean areFieldsFilled() {
        // Get the email field, radio group, and checkbox
        EditText emailEditText = welcomeBinding.emailEditText;
        CheckBox checkboxTerms = welcomeBinding.checkboxTerms;
        // RadioGroup radioGroupUserType = welcomeBinding.radioGroupUserType;

        // Check if the email field is not empty
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false; // Email is not filled
        }

        if (!isValidEmail(email)) {
            welcomeBinding.emailEditText.setError("Email inválido");
            return false; // Email is not valid
        }


        // Check if a radio button is selected
/*        int selectedRadioButtonId = radioGroupUserType.getCheckedRadioButtonId();
        if (selectedRadioButtonId == -1) {
            Toast.makeText(WelcomeActivity.this, "Please select a user type", Toast.LENGTH_SHORT).show();
            return false; // No radio button selected
        }*/

        // Check if the terms and conditions checkbox is checked
        if (!checkboxTerms.isChecked()) {
            checkboxTerms.setError("Please read and accept the terms and conditions");
            return false; // Terms and conditions not accepted
        }
        // If all fields are filled correctly
        return true;
    }

    public boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

}