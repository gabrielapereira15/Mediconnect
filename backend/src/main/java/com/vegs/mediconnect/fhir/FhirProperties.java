package com.vegs.mediconnect.fhir;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Deployment-specific values the FHIR layer cannot invent.
 *
 * Chiefly the identifier systems: a health card number means nothing
 * without the URI saying which province issued it, and that URI differs per
 * jurisdiction. They live in configuration so a clinic in another province
 * changes a YAML file rather than the mapping code.
 */
@ConfigurationProperties(prefix = "mediconnect.fhir")
@Getter
@Setter
public class FhirProperties {

    /**
     * The base this server's resources are addressed by, used for
     * Bundle.entry.fullUrl and the CapabilityStatement.
     */
    private String baseUrl = "http://localhost:8080/fhir";

    /** Name of the organisation the summaries are authored by. */
    private String organizationName = "Mediconnect Clinic";

    /**
     * Identifier system per province, keyed by two-letter code.
     *
     * These come from the Canadian URI Registry. The default below is the
     * conventional Infoway pattern and should be confirmed against the
     * registry before any real deployment — it decides whether another
     * system recognises the patient or treats them as a stranger.
     */
    private Map<String, String> healthCardSystems = Map.of(
            "ON", "https://fhir.infoway-inforoute.ca/NamingSystem/ca-on-patient-hcn",
            "BC", "https://fhir.infoway-inforoute.ca/NamingSystem/ca-bc-patient-hcn",
            "AB", "https://fhir.infoway-inforoute.ca/NamingSystem/ca-ab-patient-hcn",
            "QC", "https://fhir.infoway-inforoute.ca/NamingSystem/ca-qc-patient-hcn");

    /** The system used for this clinic's own record numbers. */
    private String localPatientSystem = "http://mediconnect.example.ca/patient-id";

    /**
     * The identifier system for practitioner licence numbers. Real
     * deployments point this at the provincial college's registry.
     */
    private String practitionerSystem = "http://mediconnect.example.ca/practitioner-id";

    /** Resolves a province code to its identifier system, or null. */
    public String healthCardSystem(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        return healthCardSystems.get(province.trim().toUpperCase());
    }
}
