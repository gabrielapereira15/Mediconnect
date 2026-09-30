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

    /**
     * Swaps what fills part of a screen, such as a Visits segment, rather
     * than the screen itself.
     *
     * Unlike loadFragment this leaves the back stack alone. A segment is a
     * view of the screen the patient is already on, not a step they took.
     * Going through loadFragment put every tap between segments on the back
     * stack, where each one was kept and rebuilt for as long as the screen
     * was open.
     *
     * The fragment is named by its class and built by the FragmentManager,
     * the same way it is rebuilt after a rotation or when the process comes
     * back. A fragment that cannot survive that fails the first time it is
     * shown, not the first time the phone is turned.
     *
     * It runs straight away rather than being queued, so the caller can ask
     * the container what it holds immediately afterwards.
     */
    public static void swapFragment(FragmentManager fragmentManager, int containerViewId,
                                    Class<? extends Fragment> fragmentClass) {
        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(containerViewId, fragmentClass, null)
                .commitNow();
    }
}
