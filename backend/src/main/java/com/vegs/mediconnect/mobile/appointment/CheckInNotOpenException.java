package com.vegs.mediconnect.mobile.appointment;

/**
 * Thrown when a patient tries to check in for a visit that is not open for
 * it: a cancelled one, or one that is not today.
 */
public class CheckInNotOpenException extends RuntimeException {

    public CheckInNotOpenException(String message) {
        super(message);
    }
}
