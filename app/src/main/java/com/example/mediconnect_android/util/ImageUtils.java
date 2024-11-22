package com.example.mediconnect_android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static void saveImageToInternalStorage(Context context, Bitmap bitmap, String filename) {
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getImageFromInternalStorage(Context context, String filename) {
        try {
            return BitmapFactory.decodeStream(context.openFileInput(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
