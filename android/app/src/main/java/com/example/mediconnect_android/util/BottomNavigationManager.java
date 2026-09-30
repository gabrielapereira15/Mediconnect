package com.example.mediconnect_android.util;

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
                show(new HomeFragment());
            } else if (id == R.id.visits_fragment) {
                show(new MedicalHistoryFragment());
            } else if (id == R.id.health_fragment) {
                show(new HealthRecordFragment());
            } else if (id == R.id.profile_fragment) {
                show(new ProfileFragment());
            } else {
                return false;
            }
            return true;
        });
    }

    /**
     * Every screen names itself in onResume, which runs after this, so a
     * title set here is overwritten before anyone sees it. The tab only
     * decides which screen appears.
     */
    private void show(Fragment fragment) {
        loadFragment(fragment);
    }

    /**
     * Replaces the current screen without adding to the back stack: a tab is
     * a place, not a step, so Back leaves the app rather than walking back
     * through every tab that was visited.
     *
     * Whatever steps were open in the tab being left are dropped first.
     * They used to stay on the stack under the new tab, so Back from
     * Profile could land on a visit opened from Home minutes earlier.
     */
    public void loadFragment(Fragment fragment) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(fragmentContainerId, fragment);
        transaction.commit();
    }

    /** Whether a screen is one of the four tabs rather than a step inside one. */
    public static boolean isTab(Fragment screen) {
        return screen instanceof HomeFragment
                || screen instanceof MedicalHistoryFragment
                || screen instanceof HealthRecordFragment
                || screen instanceof ProfileFragment;
    }
}
