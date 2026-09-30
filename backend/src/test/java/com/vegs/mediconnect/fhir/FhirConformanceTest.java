package com.vegs.mediconnect.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that what this server emits really is FHIR.
 *
 * A meta.profile is only a claim. These tests run the resources through the
 * HL7 instance validator, so "conformant" means something a reviewer can
 * re-run rather than something the README asserts.
 *
 * Scope, stated plainly: this validates against base FHIR R4 structure and
 * the invariants the specification itself defines. Validating against the CA
 * Baseline and PS-CA profiles additionally requires those packages to be
 * loaded — they are vendored under src/test/resources/fhir-packages — and a
 * terminology server for the code systems they bind to, which no test here
 * provides. Terminology warnings are therefore expected and are reported
 * rather than failed on; structural errors are failures.
 */
class FhirConformanceTest {

    private static FhirContext fhirContext;
    private static FhirValidator validator;
    private static FhirMapper mapper;
    private static FhirProperties properties;

    @BeforeAll
    static void setUp() {
        fhirContext = FhirContext.forR4();

        IValidationSupport chain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext),
                new InMemoryTerminologyServerValidationSupport(fhirContext),
                new CommonCodeSystemsTerminologyService(fhirContext),
                new SnapshotGeneratingValidationSupport(fhirContext));

        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(
                new CachingValidationSupport(chain));
        // Without a terminology server, unknown code systems would otherwise
        // be reported as errors and drown the structural findings we care
        // about. They are still visible as warnings.
        instanceValidator.setErrorForUnknownProfiles(false);

        validator = fhirContext.newValidator();
        validator.registerValidatorModule(instanceValidator);

        properties = new FhirProperties();
        mapper = new FhirMapper(properties);
    }

    // ---- fixtures -------------------------------------------------------

    private com.vegs.mediconnect.datasource.patient.Patient samplePatient() {
        var patient = new com.vegs.mediconnect.datasource.patient.Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Gabriela");
        patient.setLastName("Almeida");
        patient.setEmail("demo@mediconnect.ca");
        patient.setPhoneNumber("6475550142");
        patient.setAddress("100 Queen St W, Toronto");
        patient.setGender("female");
        patient.setBirthdate("1990-04-27");
        patient.setHealthCardNumber("1234567890");
        patient.setHealthCardProvince("ON");
        return patient;
    }

    private com.vegs.mediconnect.datasource.doctor.Doctor sampleDoctor() {
        var doctor = new com.vegs.mediconnect.datasource.doctor.Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setFirstName("Allison");
        doctor.setLastName("Cameron");
        doctor.setSpecialty("Cardiologist");
        return doctor;
    }

    private com.vegs.mediconnect.datasource.schedule.ScheduleTime sampleSlot() {
        var doctor = sampleDoctor();
        var schedule = new com.vegs.mediconnect.datasource.schedule.Schedule();
        schedule.setId(UUID.randomUUID());
        schedule.setDate(LocalDate.now().plusDays(3));
        schedule.setDoctor(doctor);
        schedule.setAvailable(true);

        var slot = new com.vegs.mediconnect.datasource.schedule.ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(LocalTime.of(10, 0));
        slot.setSchedule(schedule);
        slot.setAvailable(true);
        return slot;
    }

    private com.vegs.mediconnect.datasource.health.HealthEntry healthEntry(
            com.vegs.mediconnect.datasource.patient.Patient patient,
            com.vegs.mediconnect.datasource.health.HealthEntryType type,
            String description) {
        var entry = new com.vegs.mediconnect.datasource.health.HealthEntry();
        entry.setId(UUID.randomUUID());
        entry.setPatient(patient);
        entry.setType(type);
        entry.setDescription(description);
        entry.setActive(true);
        return entry;
    }

    // ---- individual resources -------------------------------------------

    @Test
    @DisplayName("a mapped patient is valid FHIR R4")
    void patientIsValid() {
        assertValid(mapper.toPatient(samplePatient()));
    }

    @Test
    @DisplayName("a patient carries the CA Baseline profile and a health card identifier")
    void patientCarriesCanadianIdentity() {
        Patient patient = mapper.toPatient(samplePatient());

        assertTrue(patient.getMeta().getProfile().stream()
                        .anyMatch(p -> FhirProfiles.CA_PATIENT.equals(p.getValue())),
                "expected the CA Baseline patient profile in meta.profile");

        Identifier jhn = patient.getIdentifier().stream()
                .filter(i -> i.hasType() && "JHN".equals(i.getType().getCodingFirstRep().getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no jurisdictional health number"));

        assertEquals("1234567890", jhn.getValue());
        assertEquals(properties.healthCardSystem("ON"), jhn.getSystem(),
                "the identifier system has to say which province issued the card");
    }

    @Test
    @DisplayName("a patient with no health card simply has no health card identifier")
    void patientWithoutHealthCard() {
        var source = samplePatient();
        source.setHealthCardNumber(null);
        source.setHealthCardProvince(null);

        Patient patient = mapper.toPatient(source);

        assertTrue(patient.getIdentifier().stream()
                        .noneMatch(i -> i.hasType()
                                && "JHN".equals(i.getType().getCodingFirstRep().getCode())),
                "a missing card must not produce an identifier with no value");
        assertValid(patient);
    }

    @Test
    @DisplayName("a practitioner and their role are valid FHIR R4")
    void practitionerIsValid() {
        assertValid(mapper.toPractitioner(sampleDoctor()));
        assertValid(mapper.toPractitionerRole(sampleDoctor()));
    }

    @Test
    @DisplayName("a slot is valid and reports whether it is free")
    void slotIsValid() {
        Slot slot = mapper.toSlot(sampleSlot());
        assertEquals(Slot.SlotStatus.FREE, slot.getStatus());
        assertValid(slot);
    }

    @Test
    @DisplayName("an appointment booked for someone else names them as the subject")
    void appointmentForDependant() {
        var slot = sampleSlot();
        var appointment = new com.vegs.mediconnect.datasource.appointment.Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(samplePatient());
        appointment.setDoctor(slot.getSchedule().getDoctor());
        appointment.setScheduleTime(slot);
        appointment.setCanceled(false);
        appointment.setBookedForName("Sofia Almeida");

        Appointment fhir = mapper.toAppointment(appointment);

        boolean namesDependant = fhir.getParticipant().stream()
                .anyMatch(p -> "Sofia Almeida".equals(p.getActor().getDisplay()));
        assertTrue(namesDependant, "the person attending has to appear on the appointment");
        assertValid(fhir);
    }

    // ---- the patient summary --------------------------------------------

    @Test
    @DisplayName("a patient summary is a valid FHIR document with the three mandatory sections")
    void summaryIsAValidDocument() {
        var patient = samplePatient();
        var entries = List.of(
                healthEntry(patient, com.vegs.mediconnect.datasource.health
                        .HealthEntryType.ALLERGY, "Penicillin"),
                healthEntry(patient, com.vegs.mediconnect.datasource.health
                        .HealthEntryType.MEDICATION, "Metformin 500mg"),
                healthEntry(patient, com.vegs.mediconnect.datasource.health
                        .HealthEntryType.CONDITION, "Type 2 diabetes"));

        Bundle summary = summaryFor(patient, entries);

        assertEquals(Bundle.BundleType.DOCUMENT, summary.getType());
        assertTrue(summary.getEntryFirstRep().getResource() instanceof Composition,
                "a document Bundle has to start with its Composition");

        Composition composition = (Composition) summary.getEntryFirstRep().getResource();
        assertEquals(3, composition.getSection().size());
        assertSectionPresent(composition, FhirProfiles.LOINC_SECTION_MEDICATIONS);
        assertSectionPresent(composition, FhirProfiles.LOINC_SECTION_ALLERGIES);
        assertSectionPresent(composition, FhirProfiles.LOINC_SECTION_PROBLEMS);

        assertValid(summary);
    }

    @Test
    @DisplayName("a patient with nothing recorded still gets all three sections, with a reason")
    void emptySummaryStillHasMandatorySections() {
        Bundle summary = summaryFor(samplePatient(), List.of());
        Composition composition = (Composition) summary.getEntryFirstRep().getResource();

        assertEquals(3, composition.getSection().size(),
                "PS-CA requires all three sections whether or not there is data");

        for (Composition.SectionComponent section : composition.getSection()) {
            assertTrue(section.getEntry().isEmpty());
            assertTrue(section.hasEmptyReason(),
                    "an empty mandatory section has to say why it is empty");
        }
        assertValid(summary);
    }

    @Test
    @DisplayName("every section entry resolves to a resource inside the document")
    void documentIsSelfContained() {
        var patient = samplePatient();
        Bundle summary = summaryFor(patient, List.of(
                healthEntry(patient, com.vegs.mediconnect.datasource.health
                        .HealthEntryType.ALLERGY, "Penicillin")));

        var fullUrls = summary.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getFullUrl)
                .toList();

        Composition composition = (Composition) summary.getEntryFirstRep().getResource();
        for (Composition.SectionComponent section : composition.getSection()) {
            for (Reference entry : section.getEntry()) {
                assertTrue(fullUrls.contains(entry.getReference()),
                        "a document that points outside itself is not self-contained: "
                                + entry.getReference());
            }
        }
        // The subject too, or a receiver cannot tell whose summary it is.
        assertTrue(fullUrls.contains(composition.getSubject().getReference()));
    }

    // ---- helpers --------------------------------------------------------

    /**
     * Builds a summary without a database by standing in for the repository.
     */
    private Bundle summaryFor(com.vegs.mediconnect.datasource.patient.Patient patient,
                              List<com.vegs.mediconnect.datasource.health.HealthEntry> entries) {
        var repository = new StubHealthEntryRepository(entries);
        return new PatientSummaryService(repository, mapper, properties).buildSummary(patient);
    }

    private void assertSectionPresent(Composition composition, String loincCode) {
        boolean present = composition.getSection().stream()
                .anyMatch(s -> s.getCode().getCoding().stream()
                        .anyMatch(c -> loincCode.equals(c.getCode())));
        assertTrue(present, "missing the section coded " + loincCode);
    }

    /**
     * Fails on anything the specification calls an error. Warnings —
     * overwhelmingly "could not confirm this code against a terminology
     * server" — are printed so they can be read, not asserted on.
     */
    private void assertValid(Resource resource) {
        ValidationResult result = validator.validateWithResult(resource);

        List<SingleValidationMessage> errors = result.getMessages().stream()
                .filter(m -> m.getSeverity() == ResultSeverityEnum.ERROR
                        || m.getSeverity() == ResultSeverityEnum.FATAL)
                .toList();

        if (!errors.isEmpty()) {
            StringBuilder report = new StringBuilder(
                    resource.fhirType() + " failed validation:\n");
            for (SingleValidationMessage message : errors) {
                report.append("  [").append(message.getSeverity()).append("] ")
                        .append(message.getLocationString()).append(" — ")
                        .append(message.getMessage()).append('\n');
            }
            fail(report.toString());
        }
    }
}
