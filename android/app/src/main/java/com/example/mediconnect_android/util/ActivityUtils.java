package com.example.mediconnect_android.util;

import android.content.Context;
import android.content.Intent;

public class ActivityUtils {

    public static void startActivity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }
}
