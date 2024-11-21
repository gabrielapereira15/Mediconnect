package com.example.mediconnect_android.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentUtils {

    public static void loadFragment(FragmentManager fragmentManager, int containerViewId, Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerViewId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
