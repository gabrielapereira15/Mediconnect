package com.example.mediconnect_android.activity;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.NotificationClient;
import com.example.mediconnect_android.client.NotificationClientImpl;
import com.example.mediconnect_android.databinding.ActivityMainBinding;
import com.example.mediconnect_android.fragment.EditProfileFragment;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.NotificationsFragment;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.BottomNavigationManager;
import com.example.mediconnect_android.util.DialogUtils;

/**
 * The shell the four top-level screens live in.
 *
 * The navigation drawer is gone. It held Profile, Forms, Settings and
 * Logout, which put the health record three taps deep and the way out of the
 * app behind a hamburger. Profile and Health are tabs now, Forms belongs to
 * the visit it is for, and signing out is a row on Profile — so there was
 * nothing left for a drawer to hold.
 */
public class MainActivity extends AppCompatActivity
        implements NotificationsFragment.NotificationBadgeHandler {

    private ActivityMainBinding mainBinding;
    private NotificationClient notificationClient;
    private SharedPreferences sharedPreferences;
    private BottomNavigationManager bottomNavigationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        notificationClient = new NotificationClientImpl();
        sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        setSupportActionBar(mainBinding.materialToolbar);
        // The toolbar carries no title of its own: a title set in the layout
        // is latched, and every setTitle a screen makes afterwards is
        // silently dropped. Each fragment names itself in onResume.
        setTitle(R.string.app_name);
        resetTitleBetweenScreens();
        setNavigationBottom();
        setNotificationIcon();
        listeners();
    }

    /**
     * Clears the app bar back to the app's name as each screen arrives.
     *
     * A screen that names itself does so in onResume, which runs after this;
     * one that does not would otherwise keep whatever the screen before it
     * left behind, which is how a back-office-looking title ends up over a
     * patient's visit list.
     */
    private void resetTitleBetweenScreens() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                                                      @NonNull Fragment f,
                                                      @NonNull View v,
                                                      Bundle savedInstanceState) {
                        setTitle(R.string.app_name);
                    }
                }, false);
    }

    @Override
    public void updateNotificationBadgeVisibility(boolean visible) {
        if (mainBinding != null) {
            mainBinding.notificationBadge.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setNotificationIcon() {
        String email = sharedPreferences.getString("email", "");

        // The badge is decoration; fetch it in the background so the activity
        // is interactive straight away.
        Background.run(() -> notificationClient.getNotifications(email), notifications -> {
            int count = notifications == null ? 0 : notifications.size();
            mainBinding.notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            // A dot says nothing to a screen reader, so the count goes on the
            // button's own label.
            mainBinding.notificationIcon.setContentDescription(count > 0
                    ? getString(R.string.cd_notifications_unread, count)
                    : getString(R.string.cd_notifications));
        });
    }

    private void listeners() {
        mainBinding.notificationIcon.setOnClickListener(v -> loadFragment(
                getSupportFragmentManager(), R.id.flFragment, new NotificationsFragment()));
    }

    private void setNavigationBottom() {
        bottomNavigationManager = new BottomNavigationManager(
                getSupportFragmentManager(),
                R.id.flFragment,
                mainBinding.materialToolbar,
                this::isUserDataComplete,
                this::sendToProfileForm);

        bottomNavigationManager.setupBottomNavigationListener(mainBinding.bottomNavigationView);

        // A recreated activity already has its fragment back, and the
        // navigation bar has restored which tab was selected. Loading Home
        // over the top would leave the two disagreeing — which is what
        // changing the theme used to do, since applying a night mode
        // recreates every activity.
        if (getSupportFragmentManager().findFragmentById(R.id.flFragment) != null) {
            return;
        }

        Intent intent = getIntent();
        if (intent != null && "EditProfileFragment".equals(intent.getStringExtra("target_fragment"))) {
            loadFragment(getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
            return;
        }
        bottomNavigationManager.loadFragment(new HomeFragment());
    }

    /**
     * A half-filled profile cannot book anything, so the tabs stay shut
     * until the details the clinic needs are there.
     */
    private void sendToProfileForm() {
        DialogUtils.showMessageDialog(this, getString(R.string.profile_incomplete));
        loadFragment(getSupportFragmentManager(), R.id.flFragment, new EditProfileFragment());
    }

    private boolean isUserDataComplete() {
        return notBlank("first_name") && notBlank("last_name")
                && notBlank("dob") && notBlank("phone_number");
    }

    private boolean notBlank(String key) {
        String value = sharedPreferences.getString(key, null);
        return value != null && !value.isEmpty();
    }
}
