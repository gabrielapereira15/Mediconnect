package com.example.mediconnect_android.activity;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.ActivityMainBinding;
import com.example.mediconnect_android.fragment.FormFragment;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.LogoutFragment;
import com.example.mediconnect_android.fragment.NotificationsFragment;
import com.example.mediconnect_android.fragment.ProfileFragment;
import com.example.mediconnect_android.fragment.SettingsFragment;
import com.example.mediconnect_android.util.BottomNavigationManager;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding mainBinding;
    private BottomNavigationManager bottomNavigationManager;
    ActionBarDrawerToggle mToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        init();
    }

    private void init() {
        setNavigationBottom();
        setNavigationDrawer();
        listeners();
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

        bottomNavigationManager.setupBottomNavigationListener(mainBinding.bottomNavigationView);

        bottomNavigationManager.loadFragment(new HomeFragment());
    }

    private void setNavigationDrawer() {
        mToggle = new ActionBarDrawerToggle(this, mainBinding.drawerLayout, mainBinding.materialToolbar, R.string.nav_open, R.string.nav_close);
        mainBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.light_blue));
        mToggle.syncState();

        setSupportActionBar(mainBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        View headerView = mainBinding.navView.getHeaderView(0);
        ImageView profileImageView = headerView.findViewById(R.id.iv_profile_image);

        Bitmap profileImage = getImageFromInternalStorage(MainActivity.this, "profile_image.jpg");
        if (profileImage != null) {
            profileImageView.setImageBitmap(profileImage);
        }

        mainBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment frag = null;
                int itemId = item.getItemId();
                if(itemId==R.id.nav_profile_menu){
                    frag = new ProfileFragment();
                    }
                else if(itemId==R.id.nav_forms_menu){
                    frag = new FormFragment();
                }
                else if(itemId==R.id.nav_settings_menu){
                    frag = new SettingsFragment();
                }
                else if(itemId==R.id.nav_logout_menu){
                    frag = new LogoutFragment();
                }
                else{
                    frag = new HomeFragment();
                }
                if(frag!=null){
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}