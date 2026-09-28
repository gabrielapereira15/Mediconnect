package com.example.mediconnect_android.client;

import com.example.mediconnect_android.BuildConfig;

/**
 * One place for the API location and the current bearer token.
 *
 * The base URL used to be a private field copied into six client classes, each
 * with its own literal, and two of them pointed at different hosts. It now
 * comes from BuildConfig so it can be set at build time:
 *
 *   ./gradlew assembleDebug -PapiBaseUrl=https://api.example.com
 */
public final class ApiConfig {

    private static volatile String token;

    private ApiConfig() {
    }

    /** Base URL with no trailing slash, e.g. {@code http://10.0.2.2:8080}. */
    public static String baseUrl() {
        String url = BuildConfig.API_BASE_URL;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Full URL for a path that starts with a slash. */
    public static String url(String path) {
        return baseUrl() + path;
    }

    /**
     * The token from the last successful sign-in, or null. Held in memory and
     * mirrored into SessionManager so it survives the app being closed.
     */
    public static String getToken() {
        return token;
    }

    public static void setToken(String value) {
        token = value;
    }

    public static void clearToken() {
        token = null;
    }

    public static boolean hasToken() {
        return token != null && !token.isEmpty();
    }
}
