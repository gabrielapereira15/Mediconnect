package com.example.mediconnect_android.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.databinding.ActivitySplashBinding;
import com.example.mediconnect_android.util.ActivityUtils;
import com.example.mediconnect_android.util.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // Delay in milliseconds
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Start delayed navigation
        navigateAfterDelay();
    }

    private void navigateAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);

            if (sessionManager.isLoggedIn()) {
                // User is already logged in, navigate to MainActivity
                ActivityUtils.startActivity(SplashActivity.this, MainActivity.class);
            } else {
                // User is not logged in, navigate to WelcomeActivity
                ActivityUtils.startActivity(SplashActivity.this, WelcomeActivity.class);
            }

            // Finish SplashActivity to prevent returning to it
            finish();
        }, SPLASH_DELAY);
    }
}
