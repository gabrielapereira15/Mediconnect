package com.example.mediconnect_android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs blocking work off the UI thread and delivers the result back on it.
 *
 * The API clients in this app do synchronous network I/O. Calling them straight
 * from a Fragment froze the interface for the length of the request, which with
 * a 30 second read timeout is long enough for Android to show "app isn't
 * responding". Everything that touches the network goes through here instead.
 *
 * One shared pool is used for the whole process — the previous helper created
 * a fresh ExecutorService per request and never shut it down, leaking threads.
 */
public final class Background {

    private static final String TAG = "Background";

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "mediconnect-io");
        thread.setDaemon(true);
        return thread;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Background() {
    }

    /** Called on the UI thread once the work finishes. */
    public interface OnResult<T> {
        void onResult(T result);
    }

    /** Called on the UI thread if the work threw. */
    public interface OnError {
        void onError(Exception error);
    }

    /**
     * Runs {@code work} on a background thread, then {@code onResult} on the UI
     * thread. Failures are logged and passed to {@code onError} when given.
     */
    public static <T> void run(Callable<T> work, OnResult<T> onResult, OnError onError) {
        POOL.execute(() -> {
            try {
                T result = work.call();
                MAIN.post(() -> onResult.onResult(result));
            } catch (Exception e) {
                Log.e(TAG, "Background work failed", e);
                if (onError != null) {
                    MAIN.post(() -> onError.onError(e));
                }
            }
        });
    }

    /** Same, without an error callback. */
    public static <T> void run(Callable<T> work, OnResult<T> onResult) {
        run(work, onResult, null);
    }

    /** Fire-and-forget work with no result, e.g. acknowledging a notification. */
    public static void run(Runnable work) {
        POOL.execute(() -> {
            try {
                work.run();
            } catch (Exception e) {
                Log.e(TAG, "Background work failed", e);
            }
        });
    }

    /** Posts a block onto the UI thread. */
    public static void onMain(Runnable work) {
        MAIN.post(work);
    }
}
