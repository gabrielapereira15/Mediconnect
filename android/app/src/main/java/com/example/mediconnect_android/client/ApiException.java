package com.example.mediconnect_android.client;

/**
 * A request reached the server and the server refused it.
 *
 * Distinct from being offline, which is handled by falling back to demo data.
 * This is thrown so the screen can show an error with a retry rather than an
 * empty list — a 401 or a 500 is not the same thing as "you have no
 * appointments", and showing them identically hides real failures.
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    /** The session is no longer accepted; the caller should sign in again. */
    public boolean isUnauthorised() {
        return status == 401 || status == 403;
    }
}
