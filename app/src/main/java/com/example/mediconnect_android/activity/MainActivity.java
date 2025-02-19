package com.example.mediconnect_android.activity;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.ActivityMainBinding;
import com.example.mediconnect_android.fragment.EditProfileFragment;
import com.example.mediconnect_android.fragment.FormFragment;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.LogoutFragment;
import com.example.mediconnect_android.fragment.NotificationsFragment;
import com.example.mediconnect_android.fragment.ProfileFragment;
import com.example.mediconnect_android.fragment.SettingsFragment;
import com.example.mediconnect_android.model.Notification;
import com.example.mediconnect_android.util.BottomNavigationManager;
import com.example.mediconnect_android.util.DialogUtils;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NotificationsFragment.NotificationBadgeHandler {

    ActivityMainBinding mainBinding;
    ActionBarDrawerToggle mToggle;
    NotificationClient notificationClient;
    SharedPreferences sharedPreferences;
    private BottomNavigationManager bottomNavigationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        notificationClient = new NotificationClientImpl();
        init();
    }

    private void init() {
        setNavigationBottom();
        setNavigationDrawer();
        setNotificationIcon();
        listeners();
    }

    @Override
    public void updateNotificationBadgeVisibility(boolean visible) {
        if (mainBinding != null) {
            mainBinding.notificationBadge.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setNotificationIcon() {
        String email = sharedPreferences.getString("email", "");
        List<Notification> notifications = notificationClient.getNotifications(email);

        int notificationCount = notifications != null ? notifications.size() : 0;

        View notificationBadge = findViewById(R.id.notificationBadge);

        if (notificationCount > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    private void listeners() {
        mainBinding.notificationIcon.setOnClickListener(v -> {
            loadFragment(getSupportFragmentManager(), R.id.flFragment, new NotificationsFragment());
        });
    }

    private void setNavigationBottom() {
        bottomNavigationManager = new BottomNavigationManager(
                getSupportFragmentManager(),
                R.id.flFragment,
                mainBinding.materialToolbar
        );

        mainBinding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (!isUserDataComplete()) {
                DialogUtils.showMessageDialog(this, "Please complete your profile before navigating.");
                loadFragment(getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
                return false; // Prevent navigation
            }
            return true; // Allow navigation
        });

        bottomNavigationManager.setupBottomNavigationListener(mainBinding.bottomNavigationView);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("target_fragment")) {
            String targetFragment = intent.getStringExtra("target_fragment");

            // Load the specified fragment
            if ("EditProfileFragment".equals(targetFragment)) {
                loadFragment(getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
            } else {
                // Default to HomeFragment if no specific fragment is specified
                bottomNavigationManager.loadFragment(new HomeFragment());
            }
        } else {
            // Default behavior
            bottomNavigationManager.loadFragment(new HomeFragment());
        }


    }

    private void setNavigationDrawer() {
        mToggle = new ActionBarDrawerToggle(this, mainBinding.drawerLayout, mainBinding.materialToolbar, R.string.nav_open, R.string.nav_close);
        mainBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.light_blue));
        mToggle.syncState();

        setSupportActionBar(mainBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mainBinding.navView.setNavigationItemSelectedListener(item -> {
            if (!isUserDataComplete()) {
                DialogUtils.showMessageDialog(this, "Please complete your profile before navigating.");
                loadFragment(getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
                return false; // Prevent navigation
            }
            return true; // Allow navigation
        });

        View headerView = mainBinding.navView.getHeaderView(0);
        ImageView profileImageView = headerView.findViewById(R.id.iv_profile_image);

        Bitmap profileImage = getImageFromInternalStorage(MainActivity.this, "profile_image.jpg");
        if (profileImage != null) {
            profileImageView.setImageBitmap(profileImage);
        }

        sharedPreferences = this.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String first_name = sharedPreferences.getString("first_name", "");
        String last_name = sharedPreferences.getString("last_name", "");
        String fullName = first_name + " " + last_name;

        TextView user = headerView.findViewById(R.id.textView);
        user.setText(fullName);

        mainBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment frag = null;
                int itemId = item.getItemId();
                if (itemId == R.id.nav_profile_menu) {
                    frag = new ProfileFragment();
                } else if (itemId == R.id.nav_forms_menu) {
                    frag = new FormFragment();
                } else if (itemId == R.id.nav_settings_menu) {
                    frag = new SettingsFragment();
                } else if (itemId == R.id.nav_logout_menu) {
                    frag = new LogoutFragment();
                } else {
                    frag = new HomeFragment();
                }
                if (frag != null) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.flFragment, frag);
                    ft.commit();
                    mainBinding.drawerLayout.closeDrawers();
                    return true;
                }
                return false;
            }
        });
    }

    public void updateUserName(String fullName) {
        View headerView = mainBinding.navView.getHeaderView(0);
        TextView user = headerView.findViewById(R.id.textView);
        user.setText(fullName);
    }

    private boolean isUserDataComplete() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String firstName = sharedPreferences.getString("first_name", null);
        String lastName = sharedPreferences.getString("last_name", null);
        String dob = sharedPreferences.getString("dob", null);
        String phoneNumber = sharedPreferences.getString("phone_number", null);

        // Check if any of the required fields is null or empty
        return firstName != null && !firstName.isEmpty()
                && lastName != null && !lastName.isEmpty()
                && dob != null && !dob.isEmpty()
                && phoneNumber != null && !phoneNumber.isEmpty();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}