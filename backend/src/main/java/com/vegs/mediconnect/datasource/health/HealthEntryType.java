package com.vegs.mediconnect.datasource.health;

/**
 * The three things a patient summary must carry.
 *
 * PS-CA makes Medications, Allergies and Problems mandatory sections of the
 * summary document, so these are exactly the categories the app has to be
 * able to collect. Anything the patient tells us that is not one of these
 * has no place to go in the summary.
 */
public enum HealthEntryType {

    /** Something the patient reacts badly to. Maps to AllergyIntolerance. */
    ALLERGY,

    /** Something the patient takes. Maps to MedicationStatement. */
    MEDICATION,

    /** An ongoing problem being monitored. Maps to Condition. */
    CONDITION
}
