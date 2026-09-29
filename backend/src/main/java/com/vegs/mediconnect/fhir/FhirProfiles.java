package com.vegs.mediconnect.fhir;

/**
 * The canonical URLs this server claims conformance to.
 *
 * Every value here was read from the published package rather than typed
 * from memory, because a profile URL that does not resolve is worse than no
 * profile URL at all — it claims a conformance that cannot be checked.
 *
 * Sources:
 *  - CA Core+ 1.2.1-dft, package ca.infoway.io.core
 *  - CA Baseline 1.2.0, package hl7.fhir.ca.baseline
 *  - PS-CA 2.1.1-DFT, package ca.infoway.io.psca
 * All three build on FHIR R4 (4.0.1).
 *
 * CA Core+ and CA Baseline are both claimed where both apply. They are not
 * rivals: Baseline is HL7 Canada's floor for a resource, Core+ is Infoway's
 * expression of the Canadian Core Data for Interoperability on top of it,
 * and meta.profile is a list precisely so a resource can say it satisfies
 * more than one.
 */
public final class FhirProfiles {

    private FhirProfiles() {
    }

    // ---- CA Core+ (Canada Health Infoway) -------------------------------

    private static final String CA_CORE = "http://fhir.infoway-inforoute.ca/cacore/StructureDefinition/";

    public static final String CACORE_PATIENT = CA_CORE + "patient-ca-core";
    public static final String CACORE_PRACTITIONER = CA_CORE + "practitioner-ca-core";
    public static final String CACORE_PRACTITIONER_ROLE = CA_CORE + "practitionerRole-ca-core";
    public static final String CACORE_ORGANIZATION = CA_CORE + "organization-ca-core";

    /**
     * The one scheduling resource Canada profiles.
     *
     * CA Baseline has no Appointment, Schedule or Slot profile, so the
     * earlier version of this file claimed none for any of them. That was
     * only true of Baseline: CA Core+ does profile Appointment. Schedule and
     * Slot remain unprofiled, and are served as plain R4.
     */
    public static final String CACORE_APPOINTMENT = CA_CORE + "appointment-ca-core";

    // ---- CA Baseline (HL7 Canada) ---------------------------------------

    private static final String CA_BASELINE = "http://hl7.org/fhir/ca/baseline/StructureDefinition/";

    public static final String CA_PATIENT = CA_BASELINE + "profile-patient";
    public static final String CA_PRACTITIONER = CA_BASELINE + "profile-practitioner";
    public static final String CA_PRACTITIONER_ROLE = CA_BASELINE + "profile-practitionerrole";
    public static final String CA_ORGANIZATION = CA_BASELINE + "profile-organization";

    // ---- PS-CA (Canada Health Infoway) ----------------------------------

    private static final String PS_CA = "http://fhir.infoway-inforoute.ca/io/psca/StructureDefinition/";

    public static final String PSCA_BUNDLE = PS_CA + "bundle-ca-ps";
    public static final String PSCA_COMPOSITION = PS_CA + "composition-ca-ps";
    public static final String PSCA_PATIENT = PS_CA + "patient-ca-ps";
    public static final String PSCA_ALLERGY = PS_CA + "allergyintolerance-ca-ps";
    public static final String PSCA_CONDITION = PS_CA + "condition-ca-ps";
    public static final String PSCA_MEDICATION_STATEMENT = PS_CA + "medicationstatement-ca-ps";

    // ---- Terminology used by the summary --------------------------------

    public static final String LOINC = "http://loinc.org";
    public static final String SNOMED = "http://snomed.info/sct";

    /** LOINC 60591-5, "Patient summary Document" — the Composition.type. */
    public static final String LOINC_PATIENT_SUMMARY = "60591-5";

    /**
     * The three sections PS-CA requires. A summary without all three is not
     * a valid PS-CA document, even when the patient has nothing to report —
     * hence the empty-reason handling in the builder.
     */
    public static final String LOINC_SECTION_MEDICATIONS = "10160-0";
    public static final String LOINC_SECTION_ALLERGIES = "48765-2";
    public static final String LOINC_SECTION_PROBLEMS = "11450-4";

    /** HL7 list-empty-reason, used when a mandatory section has no entries. */
    public static final String LIST_EMPTY_REASON =
            "http://terminology.hl7.org/CodeSystem/list-empty-reason";
}
