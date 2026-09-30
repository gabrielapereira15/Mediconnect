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
import com.example.mediconnect_android.data.DemoMode;
import com.example.mediconnect_android.databinding.ActivityMainBinding;
import com.example.mediconnect_android.fragment.EditProfileFragment;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.NotificationsFragment;
import com.example.mediconnect_android.util.BottomNavigationManager;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.UnreadMessages;

/**
 * The shell the four top-level screens live in.
 *
 * The navigation drawer is gone. It held Profile, Forms, Settings and
 * Logout, which put the health record three taps deep and the way out of the
 * app behind a hamburger. Profile and Health are tabs now, Forms belongs to
 * the visit it is for, and signing out is a row on Profile — so there was
 * nothing left for a drawer to hold.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;
    private NotificationClient notificationClient;
    private SharedPreferences sharedPreferences;
    private BottomNavigationManager bottomNavigationManager;

    /** Held so the same instance can be let go of in onDestroy. */
    private final UnreadMessages.Listener bell = this::bindBell;

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
        watchDemoMode();
        setNavigationBottom();
        UnreadMessages.observe(bell);
        listeners();
    }

    /**
     * Counts the unread messages again each time the app comes to the front.
     *
     * The count used to be fetched once, in onCreate, so a message that
     * arrived while the app sat in the background never reached the bell.
     * onResume also runs straight after onCreate, which covers the first
     * launch.
     */
    @Override
    protected void onResume() {
        super.onResume();
        UnreadMessages.refresh(notificationClient, sharedPreferences.getString("email", ""));
    }

    /**
     * The count outlives the activity, and a theme change recreates it, so
     * a listener left behind would keep the old views alive.
     */
    @Override
    protected void onDestroy() {
        UnreadMessages.stopObserving(bell);
        super.onDestroy();
    }

    /**
     * Shows a banner whenever the app is falling back to sample data.
     *
     * The flag flips on whichever background thread made the failed
     * request, so the update is posted to the main thread before it touches
     * the view.
     */
    private void watchDemoMode() {
        bindDemoBanner();
        DemoMode.observe(() -> runOnUiThread(this::bindDemoBanner));
    }

    private void bindDemoBanner() {
        if (mainBinding != null) {
            mainBinding.demoBanner.setVisibility(
                    DemoMode.isActive() ? View.VISIBLE : View.GONE);
        }
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
                        // Screens only. A bottom sheet is shown through the
                        // same manager, and resetting the title under it
                        // left the screen behind renamed once it closed.
                        if (f.getId() != R.id.flFragment) {
                            return;
                        }
                        setTitle(R.string.app_name);
                        bindUpArrow(f);
                    }
                }, false);
    }

    /**
     * A back arrow on every screen that is a step inside a tab.
     *
     * Decided by what the screen is rather than by the back stack's depth:
     * a tab is a place, so it never gets one, whatever the stack holds.
     * Before this, a visit or a booking step had no way back on screen and
     * relied on the system gesture, which not everyone knows is there.
     */
    private void bindUpArrow(Fragment screen) {
        if (BottomNavigationManager.isTab(screen)) {
            mainBinding.materialToolbar.setNavigationIcon(null);
            return;
        }
        mainBinding.materialToolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        mainBinding.materialToolbar.setNavigationContentDescription(R.string.cd_back);
        mainBinding.materialToolbar.setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * The app bar's bell. Its label follows the count as well as its dot:
     * only the dot used to change, so after "Mark all read" TalkBack still
     * announced the old number.
     */
    private void bindBell(int unread) {
        if (mainBinding != null) {
            UnreadMessages.bindBell(mainBinding.notificationIcon, mainBinding.notificationBadge, unread);
        }
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
