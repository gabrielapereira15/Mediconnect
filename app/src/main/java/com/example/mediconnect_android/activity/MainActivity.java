package com.example.mediconnect_android.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.ActivityMainBinding;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.util.BottomNavigationManager;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding mainBinding;
    private BottomNavigationManager bottomNavigationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        bottomNavigationManager = new BottomNavigationManager(
                getSupportFragmentManager(),
                R.id.flFragment,
                mainBinding.materialToolbar
        );

        bottomNavigationManager.setupBottomNavigationListener(mainBinding.bottomNavigationView);

        init();
        bottomNavigationManager.loadFragment(new HomeFragment());
    }

    private void init() {
    }
}