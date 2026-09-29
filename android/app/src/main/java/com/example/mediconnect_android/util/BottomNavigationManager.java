package com.example.mediconnect_android.util;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.fragment.HealthRecordFragment;
import com.example.mediconnect_android.fragment.HomeFragment;
import com.example.mediconnect_android.fragment.MedicalHistoryFragment;
import com.example.mediconnect_android.fragment.ProfileFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.function.BooleanSupplier;

/**
 * Turns a tap on the bottom navigation into a screen.
 *
 * Four destinations now, not three: the health record used to live under
 * Profile and is a tab of its own, since it is the thing a patient is most
 * often asked for and the least likely to go hunting for.
 *
 * The "is the profile complete" guard used to be a second listener set on
 * the same view from the activity, which silently replaced this one —
 * navigation worked only because that listener happened to be installed
 * first and returned true. It is passed in here instead, so there is one
 * listener and one place deciding whether a tap goes through.
 */
public class BottomNavigationManager {

    private final FragmentManager fragmentManager;
    private final int fragmentContainerId;
    private final MaterialToolbar toolbar;
    private final BooleanSupplier canNavigate;
    private final Runnable onBlocked;

    public BottomNavigationManager(FragmentManager fragmentManager,
                                   int fragmentContainerId,
                                   MaterialToolbar toolbar,
                                   BooleanSupplier canNavigate,
                                   Runnable onBlocked) {
        this.fragmentManager = fragmentManager;
        this.fragmentContainerId = fragmentContainerId;
        this.toolbar = toolbar;
        this.canNavigate = canNavigate;
        this.onBlocked = onBlocked;
    }

    public void setupBottomNavigationListener(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (!canNavigate.getAsBoolean()) {
                onBlocked.run();
                return false;
            }

            int id = item.getItemId();
            if (id == R.id.home_fragment) {
                show(new HomeFragment(), R.string.app_name);
            } else if (id == R.id.visits_fragment) {
                show(new MedicalHistoryFragment(), R.string.nav_visits);
            } else if (id == R.id.health_fragment) {
                show(new HealthRecordFragment(), R.string.health_record_title);
            } else if (id == R.id.profile_fragment) {
                show(new ProfileFragment(), R.string.nav_profile);
            } else {
                return false;
            }
            return true;
        });
    }

    private void show(Fragment fragment, @StringRes int titleRes) {
        toolbar.setTitle(titleRes);
        loadFragment(fragment);
    }

    /**
     * Replaces the current screen without adding to the back stack: a tab is
     * a place, not a step, so Back leaves the app rather than walking back
     * through every tab that was visited.
     */
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(fragmentContainerId, fragment);
        transaction.commit();
    }
}
