package com.vegs.mediconnect.mobile.patient;

/**
 * Thrown when a profile save carries a health card the clinic could not use:
 * a number in no shape any province issues, or no province to read it by.
 *
 * The message is written for the patient, because the app shows it as it is.
 */
public class InvalidHealthCardException extends RuntimeException {

    public InvalidHealthCardException(String message) {
        super(message);
    }
}
