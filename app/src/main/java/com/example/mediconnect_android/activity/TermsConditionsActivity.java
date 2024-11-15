package com.example.mediconnect_android.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.ActivityOtpactivityBinding;
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
        binding.backButton.setOnClickListener(v -> {
            transitionToWelcomeActivity(false);
        });
        binding.agreeButton.setOnClickListener(v -> {
            transitionToWelcomeActivity(true);
        });
        binding.disagreeButton.setOnClickListener(v -> {
            transitionToWelcomeActivity(false);
        });
    }

    private void transitionToWelcomeActivity(Boolean isTermsAgreed) {
        Intent intent = new Intent(TermsConditionsActivity.this, WelcomeActivity.class);
        intent.putExtra("TERMS_AGREED", isTermsAgreed);
        startActivity(intent);
        finish();
    }
}