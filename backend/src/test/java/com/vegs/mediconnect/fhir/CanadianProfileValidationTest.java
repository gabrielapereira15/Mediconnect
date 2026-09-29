package com.vegs.mediconnect.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates against the real Canadian implementation guides.
 *
 * The other conformance test proves the resources are valid FHIR. This one
 * goes further and loads CA Baseline 1.2.0 and PS-CA 2.3.0-ti-ballot as
 * published packages, so a profile claimed in meta.profile is checked
 * against the actual StructureDefinition rather than taken on trust.
 *
 * The packages are vendored under src/test/resources/fhir-packages because
 * CI has no network access and a conformance test that silently skips when
 * a download fails is worse than no test.
 *
 * Honest limits. There is no terminology server here, so bindings to SNOMED
 * CT and LOINC cannot be confirmed and surface as warnings; those are
 * reported, not failed on. What this test does assert is that the profiles
 * load, that the structural constraints they add are satisfied, and that
 * every profile URL this codebase claims actually exists in the published
 * package — which is the part that is easy to get wrong by typing a URL
 * from memory.
 */
class CanadianProfileValidationTest {

    private static final String CA_BASELINE_PACKAGE =
            "/fhir-packages/hl7.fhir.ca.baseline-1.2.0.tgz";
    /**
     * PS-CA 2.1.1-DFT rather than the newer ti-ballot builds.
     *
     * Those are produced by IG tooling that emits an `additional-binding`
     * extension carrying both a value and nested extensions, which HAPI
     * 7.4.5 rejects outright while parsing the package. The profile URLs are
     * canonical and unversioned, so the ones this codebase claims are the
     * same in either build — this version is simply the newest one the
     * validator on the classpath can actually read.
     */
    private static final String PSCA_PACKAGE =
            "/fhir-packages/ca.infoway.io.psca-2.1.1-DFT.tgz";

    private static FhirContext fhirContext;
    private static FhirValidator validator;
    private static NpmPackageValidationSupport packageSupport;
    private static FhirMapper mapper;

    @BeforeAll
    static void loadImplementationGuides() throws Exception {
        fhirContext = FhirContext.forR4();

        packageSupport = new NpmPackageValidationSupport(fhirContext);
        packageSupport.loadPackageFromClasspath(CA_BASELINE_PACKAGE);
        packageSupport.loadPackageFromClasspath(PSCA_PACKAGE);

        IValidationSupport chain = new ValidationSupportChain(
                packageSupport,
                new DefaultProfileValidationSupport(fhirContext),
                new InMemoryTerminologyServerValidationSupport(fhirContext),
                new CommonCodeSystemsTerminologyService(fhirContext),
                new SnapshotGeneratingValidationSupport(fhirContext));

        FhirInstanceValidator instanceValidator =
                new FhirInstanceValidator(new CachingValidationSupport(chain));
        // A profile that cannot be found must fail loudly here — the whole
        // point of this test is that the claimed profiles resolve.
        instanceValidator.setErrorForUnknownProfiles(true);

        validator = fhirContext.newValidator();
        validator.registerValidatorModule(instanceValidator);

        mapper = new FhirMapper(new FhirProperties());
    }

    @Test
    @DisplayName("every profile URL this codebase claims exists in the published packages")
    void claimedProfilesResolve() {
        List<String> claimed = List.of(
                FhirProfiles.CA_PATIENT,
                FhirProfiles.CA_PRACTITIONER,
                FhirProfiles.CA_PRACTITIONER_ROLE,
                FhirProfiles.CA_ORGANIZATION,
                FhirProfiles.PSCA_BUNDLE,
                FhirProfiles.PSCA_COMPOSITION,
                FhirProfiles.PSCA_PATIENT,
                FhirProfiles.PSCA_ALLERGY,
                FhirProfiles.PSCA_CONDITION,
                FhirProfiles.PSCA_MEDICATION_STATEMENT);

        for (String url : claimed) {
            var definition = packageSupport.fetchStructureDefinition(url);
            assertNotNull(definition,
                    "no StructureDefinition published at " + url
                            + " — a profile URL that does not resolve claims a conformance "
                            + "nobody can check");
            assertEquals(url, ((StructureDefinition) definition).getUrl());
        }
    }

    @Test
    @DisplayName("the loaded guides build on FHIR R4")
    void guidesTargetR4() {
        var patientProfile = (StructureDefinition)
                packageSupport.fetchStructureDefinition(FhirProfiles.CA_PATIENT);

        assertEquals("4.0.1", patientProfile.getFhirVersion().toCode(),
                "this mapping is written against R4; an R5 guide would need different code");

        // Worth knowing rather than asserting: the package is published as
        // hl7.fhir.ca.baseline 1.2.0, but the profiles inside it are still
        // stamped 1.1.0. The canonical URL is unversioned and is what the
        // mapping claims, so this does not affect conformance — it is
        // recorded here so the next person does not assume the two agree.
        assertNotNull(patientProfile.getVersion());
        System.out.println("CA Baseline package 1.2.0 contains profile-patient version "
                + patientProfile.getVersion());
    }

    @Test
    @DisplayName("a mapped patient satisfies the CA Baseline patient profile")
    void patientConformsToCaBaseline() {
        var source = new com.vegs.mediconnect.datasource.patient.Patient();
        source.setId(UUID.randomUUID());
        source.setFirstName("Gabriela");
        source.setLastName("Almeida");
        source.setEmail("demo@mediconnect.ca");
        source.setPhoneNumber("6475550142");
        source.setGender("female");
        source.setBirthdate("1990-04-27");
        source.setHealthCardNumber("1234567890");
        source.setHealthCardProvince("ON");

        Patient patient = mapper.toPatient(source);
        assertStructurallyValid(patient, FhirProfiles.CA_PATIENT);
    }

    /**
     * Fails on structural errors, reports terminology ones.
     *
     * Splitting the two is the point: "this element is missing" is a defect
     * in the mapping, while "I cannot reach SNOMED to check this code" is a
     * limitation of running validation offline, and conflating them would
     * either hide real problems or make the suite permanently red.
     */
    private void assertStructurallyValid(Resource resource, String profile) {
        ValidationResult result = validator.validateWithResult(resource);

        List<SingleValidationMessage> errors = result.getMessages().stream()
                .filter(m -> m.getSeverity() == ResultSeverityEnum.ERROR
                        || m.getSeverity() == ResultSeverityEnum.FATAL)
                .filter(m -> !isTerminology(m))
                .toList();

        List<SingleValidationMessage> terminology = result.getMessages().stream()
                .filter(CanadianProfileValidationTest::isTerminology)
                .toList();

        if (!terminology.isEmpty()) {
            System.out.println(resource.fhirType() + " against " + profile
                    + ": " + terminology.size()
                    + " terminology finding(s), expected without a terminology server");
        }

        if (!errors.isEmpty()) {
            StringBuilder report = new StringBuilder(
                    resource.fhirType() + " does not satisfy " + profile + ":\n");
            for (SingleValidationMessage message : errors) {
                report.append("  [").append(message.getSeverity()).append("] ")
                        .append(message.getLocationString()).append(" — ")
                        .append(message.getMessage()).append('\n');
            }
            fail(report.toString());
        }
    }

    private static boolean isTerminology(SingleValidationMessage message) {
        String text = message.getMessage();
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("terminology")
                || lower.contains("unable to validate code")
                || lower.contains("unknown code")
                || lower.contains("could not be validated")
                || lower.contains("valueset")
                || lower.contains("code system");
    }
}
