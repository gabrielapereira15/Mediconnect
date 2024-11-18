package com.example.mediconnect_android.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.databinding.ActivitySplashBinding;
import com.example.mediconnect_android.util.ActivityUtils;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // 3 seconds
    ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityUtils.startActivity(SplashActivity.this, WelcomeActivity.class);
            }
        }, SPLASH_DELAY);
    }
}