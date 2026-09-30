package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Thin wrapper over OkHttp for the API clients.
 *
 * These calls block, by design — callers run them through {@link
 * com.example.mediconnect_android.util.Background}. The previous version
 * submitted each request to a brand new ExecutorService and then immediately
 * blocked on get(), which leaked a thread per request while still freezing
 * whichever thread called it.
 *
 * A single OkHttpClient is shared, as OkHttp intends: it pools connections and
 * threads internally, and creating one per client class threw that away.
 */
public final class OkHttpClientHelper {

    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.get("application/json");

    /** Status used when the server could not be reached at all. */
    public static final int STATUS_OFFLINE = 0;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // Attach the bearer token, when signed in, to every request.
            .addInterceptor(chain -> {
                Request original = chain.request();
                if (!ApiConfig.hasToken()) {
                    return chain.proceed(original);
                }
                return chain.proceed(original.newBuilder()
                        .header("Authorization", "Bearer " + ApiConfig.getToken())
                        .build());
            })
            .build();

    private OkHttpClientHelper() {
    }

    public static ApiGenericResponse get(String url) {
        return execute(new Request.Builder().url(url).get().build());
    }

    public static ApiGenericResponse post(String url, String jsonBody) {
        return execute(new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON))
                .build());
    }

    public static ApiGenericResponse put(String url) {
        return put(url, "");
    }

    public static ApiGenericResponse put(String url, String jsonBody) {
        return execute(new Request.Builder()
                .url(url)
                .put(RequestBody.create(jsonBody, JSON))
                .build());
    }

    /**
     * Performs the request and always returns a response object — a network
     * failure comes back as unsuccessful rather than as an exception, so the
     * clients can fall back to demo data instead of crashing.
     */
    private static ApiGenericResponse execute(Request request) {
        var result = new ApiGenericResponse();

        try (Response response = CLIENT.newCall(request).execute()) {
            result.setSuccess(response.isSuccessful());
            result.setStatus(response.code());
            result.setResponseBody(response.body() == null ? "" : response.body().string());

            if (!response.isSuccessful()) {
                Log.w(TAG, request.method() + " " + request.url()
                        + " returned " + response.code());
            }
        } catch (IOException e) {
            // Unreachable server, DNS failure, timeout — expected when the
            // backend is not running, which is why this is not fatal.
            Log.w(TAG, "Could not reach " + request.url() + ": " + e.getMessage());
            result.setSuccess(false);
            result.setStatus(STATUS_OFFLINE);
            result.setResponseBody("");
        }

        return result;
    }

    /** True when the failure was "no server" rather than an error status. */
    public static boolean isOffline(ApiGenericResponse response) {
        return response != null && !response.isSuccess() && response.getStatus() == STATUS_OFFLINE;
    }
}
