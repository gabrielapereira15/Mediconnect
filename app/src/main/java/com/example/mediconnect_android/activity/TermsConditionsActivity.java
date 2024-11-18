package com.example.mediconnect_android.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.databinding.ActivityTermsConditionsBinding;

public class TermsConditionsActivity extends AppCompatActivity {

    ActivityTermsConditionsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsConditionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        listeners();
    }

    private void listeners() {
        Intent intent = getIntent();
        Intent intentWelcome = new Intent(TermsConditionsActivity.this, WelcomeActivity.class);

        if (intent != null && intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            intentWelcome.putExtra("email", email);
        }

        binding.backButton.setOnClickListener(v -> {
            if (intent != null && intent.hasExtra("TERMS_AGREED")) {
                boolean isTermsAgreed = intent.getBooleanExtra("TERMS_AGREED", false);
                intentWelcome.putExtra("TERMS_AGREED", isTermsAgreed);
            }
            startActivity(intentWelcome);
        });

        binding.agreeButton.setOnClickListener(v -> {
            intentWelcome.putExtra("TERMS_AGREED", true);
            startActivity(intentWelcome);
        });

        binding.disagreeButton.setOnClickListener(v -> {
            intentWelcome.putExtra("TERMS_AGREED", false);
            startActivity(intentWelcome);
        });
    }
}