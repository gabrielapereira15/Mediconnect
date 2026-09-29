package com.example.mediconnect_android.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;

public class FragmentUtils {

    private FragmentUtils() {
    }

    /**
     * Swaps the visible fragment.
     *
     * Every screen change in the app goes through here, so the transition is
     * set in one place. Without it screens replaced each other instantly, with
     * nothing to suggest which direction you had moved.
     *
     * Android scales these durations by the system animation setting, so a
     * device with animations turned off still gets an instant swap.
     */
    public static void loadFragment(FragmentManager fragmentManager, int containerViewId,
                                    Fragment fragment) {
        loadFragment(fragmentManager, containerViewId, fragment, null);
    }

    /**
     * The same, with a name on the back-stack entry.
     *
     * A flow that ends somewhere else — booking ends on a confirmation —
     * has to be able to remove its own steps, or the back gesture walks the
     * patient into a review screen for an appointment they already hold.
     */
    public static void loadFragment(FragmentManager fragmentManager, int containerViewId,
                                    Fragment fragment, String backStackName) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fragment_enter,
                R.anim.fragment_exit,
                R.anim.fragment_pop_enter,
                R.anim.fragment_pop_exit);
        transaction.replace(containerViewId, fragment);
        transaction.addToBackStack(backStackName);
        transaction.commit();
    }
}
