package com.vegs.mediconnect.fhir;

import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Builds a pan-Canadian Patient Summary (PS-CA) for one patient.
 *
 * A patient summary is a FHIR *document*: a Bundle of type "document" whose
 * first entry is a Composition acting as the cover page, with every other
 * resource referenced from one of its sections. That shape is what lets
 * another Canadian system open it without knowing anything about this one.
 *
 * PS-CA makes three sections mandatory — medications, allergies and
 * problems. They are present even when the patient has reported nothing, in
 * which case the section carries an emptyReason instead of entries, because
 * "no known allergies" and "we never asked" are clinically different and the
 * standard insists on saying which.
 *
 * What this is not: a certified or connected integration. It produces and
 * validates a conformant document. Exchanging it with a provincial system
 * additionally needs SMART on FHIR, onboarding and Infoway conformance
 * testing, none of which live in this repository.
 */
@Service
@RequiredArgsConstructor
public class PatientSummaryService {

    private final HealthEntryRepository healthEntryRepository;
    private final FhirMapper mapper;
    private final FhirProperties properties;

    /**
     * The whole summary as one self-contained document Bundle.
     */
    public Bundle buildSummary(Patient patient) {
        List<HealthEntry> entries = healthEntryRepository
                .findAllByPatientOrderByDateCreatedAsc(patient);

        org.hl7.fhir.r4.model.Patient subject = mapper.toPatient(patient);
        // PS-CA slices Bundle.entry by the profile each resource declares,
        // so the patient inside a summary has to say it is a PS-CA patient
        // and not only a CA Baseline one. It conforms to both, and
        // meta.profile is a list, so both are declared.
        subject.getMeta().addProfile(FhirProfiles.PSCA_PATIENT);
        Organization author = mapper.organization();
        Date now = new Date();

        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.getMeta().addProfile(FhirProfiles.PSCA_BUNDLE);
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.setTimestamp(now);
        // A document Bundle needs a persistent identifier of its own so a
        // receiver can tell two summaries of the same patient apart.
        bundle.setIdentifier(new Identifier()
                .setSystem("urn:ietf:rfc:3986")
                .setValue("urn:uuid:" + bundle.getId()));

        Composition composition = composition(subject, author, now);

        // Composition must be the first entry of a document Bundle.
        addEntry(bundle, composition);

        addSection(bundle, composition, entries, HealthEntryType.MEDICATION,
                FhirProfiles.LOINC_SECTION_MEDICATIONS, "Medication Summary",
                mapper::toMedicationStatement);
        addSection(bundle, composition, entries, HealthEntryType.ALLERGY,
                FhirProfiles.LOINC_SECTION_ALLERGIES, "Allergies and Intolerances",
                mapper::toAllergyIntolerance);
        addSection(bundle, composition, entries, HealthEntryType.CONDITION,
                FhirProfiles.LOINC_SECTION_PROBLEMS, "Problem List",
                mapper::toCondition);

        addEntry(bundle, subject);
        addEntry(bundle, author);

        return bundle;
    }

    private Composition composition(org.hl7.fhir.r4.model.Patient subject,
                                    Organization author, Date now) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.getMeta().addProfile(FhirProfiles.PSCA_COMPOSITION);
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirProfiles.LOINC)
                .setCode(FhirProfiles.LOINC_PATIENT_SUMMARY)
                .setDisplay("Patient summary Document")));
        composition.setDate(now);
        composition.setTitle("Patient Summary");
        composition.setSubject(reference(subject));
        composition.addAuthor(reference(author));
        return composition;
    }

    /**
     * Adds one section, with its resources, to both the Composition and the
     * Bundle.
     *
     * The generic parameter keeps the three calls above honest: each entry
     * type has its own FHIR resource, and the mapper decides which.
     */
    private <T extends Resource> void addSection(
            Bundle bundle, Composition composition, List<HealthEntry> entries,
            HealthEntryType type, String loincCode, String title,
            Function<HealthEntry, T> toResource) {

        Composition.SectionComponent section = composition.addSection();
        section.setTitle(title);
        section.setCode(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirProfiles.LOINC)
                .setCode(loincCode)
                .setDisplay(title)));

        List<T> resources = new ArrayList<>();
        for (HealthEntry entry : entries) {
            if (entry.getType() == type) {
                resources.add(toResource.apply(entry));
            }
        }

        if (resources.isEmpty()) {
            // Mandatory section, nothing to put in it. "unavailable" says
            // the information was not obtained, which is the truthful code
            // here — the app never asked the patient to confirm they have
            // none, so claiming "nilknown" would overstate what we know.
            section.setEmptyReason(new CodeableConcept().addCoding(new Coding()
                    .setSystem(FhirProfiles.LIST_EMPTY_REASON)
                    .setCode("unavailable")
                    .setDisplay("Unavailable")));
            section.setText(narrative("No information was recorded for " + title.toLowerCase() + "."));
            return;
        }

        StringBuilder narrative = new StringBuilder("<ul>");
        for (T resource : resources) {
            section.addEntry(reference(resource));
            addEntry(bundle, resource);
            narrative.append("<li>").append(escape(describe(resource))).append("</li>");
        }
        narrative.append("</ul>");
        section.setText(narrative(narrative.toString()));
    }

    /**
     * A human-readable line for the section narrative.
     *
     * Every section of a document must carry text a clinician can read
     * without a FHIR parser, so this is not decoration — it is what makes
     * the document usable if the structured entries cannot be processed.
     */
    private String describe(Resource resource) {
        if (resource instanceof AllergyIntolerance allergy) {
            return allergy.getCode().getText();
        }
        if (resource instanceof Condition condition) {
            return condition.getCode().getText();
        }
        if (resource instanceof MedicationStatement statement
                && statement.getMedication() instanceof CodeableConcept concept) {
            return concept.getText();
        }
        return resource.getIdElement().getIdPart();
    }

    private Narrative narrative(String html) {
        Narrative text = new Narrative();
        text.setStatus(Narrative.NarrativeStatus.GENERATED);
        text.setDivAsString("<div xmlns=\"http://www.w3.org/1999/xhtml\">" + html + "</div>");
        return text;
    }

    /**
     * Resources in a document Bundle are addressed by their fullUrl, so
     * references between them have to use the same urn:uuid form rather than
     * a "Patient/123" path that would point outside the document.
     */
    private Reference reference(Resource resource) {
        return new Reference(fullUrl(resource));
    }

    private void addEntry(Bundle bundle, Resource resource) {
        bundle.addEntry()
                .setFullUrl(fullUrl(resource))
                .setResource(resource);
    }

    private String fullUrl(Resource resource) {
        return properties.getBaseUrl() + "/" + resource.fhirType()
                + "/" + resource.getIdElement().getIdPart();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
