package com.vegs.mediconnect.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.vegs.mediconnect.auth.AuthInterceptor;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * A read-only FHIR R4 facade over the clinic's data.
 *
 * Separate from the mobile API on purpose. The mobile endpoints are shaped
 * for one app's screens and will keep changing with them; this one is shaped
 * by the standard, so another system can read it without knowing anything
 * about Mediconnect. The two share the database and nothing else.
 *
 * Read-only for now: accepting inbound FHIR means resolving whether an
 * incoming Patient is someone already on file, which is an identity-matching
 * problem rather than a parsing one, and is not attempted here.
 */
@RestController
@RequestMapping(value = "/fhir", produces = FhirController.FHIR_JSON)
@RequiredArgsConstructor
// Mapping an appointment walks scheduleTime -> schedule -> doctor, and those
// associations are lazy. Without a transaction open while the response is
// built, the first of them throws LazyInitializationException and the whole
// endpoint returns a 500.
@Transactional(readOnly = true)
public class FhirController {

    public static final String FHIR_JSON = "application/fhir+json";

    private final FhirContext fhirContext;
    private final FhirMapper mapper;
    private final FhirProperties properties;
    private final PatientSummaryService summaryService;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;

    // ---- conformance ----------------------------------------------------

    /**
     * What this server supports, at the address every FHIR client tries
     * first. Public: a client has to be able to discover the server before
     * it has any reason to authenticate.
     */
    @GetMapping("/metadata")
    public ResponseEntity<String> metadata() {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
        statement.setDate(new java.util.Date());
        statement.setPublisher(properties.getOrganizationName());
        statement.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);
        statement.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        statement.addFormat("application/fhir+json");
        statement.setImplementation(new CapabilityStatement
                .CapabilityStatementImplementationComponent()
                .setDescription("Mediconnect FHIR facade")
                .setUrl(properties.getBaseUrl()));

        CapabilityStatement.CapabilityStatementRestComponent rest = statement.addRest();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

        // CA Core+ is named as the profile and CA Baseline as an additional
        // supported one, since CapabilityStatement allows exactly one of the
        // former and any number of the latter.
        supportedResource(rest, "Patient", FhirProfiles.CACORE_PATIENT, FhirProfiles.CA_PATIENT);
        supportedResource(rest, "Practitioner", FhirProfiles.CACORE_PRACTITIONER,
                FhirProfiles.CA_PRACTITIONER);
        supportedResource(rest, "PractitionerRole", FhirProfiles.CACORE_PRACTITIONER_ROLE,
                FhirProfiles.CA_PRACTITIONER_ROLE);
        supportedResource(rest, "Organization", FhirProfiles.CACORE_ORGANIZATION,
                FhirProfiles.CA_ORGANIZATION);
        supportedResource(rest, "Appointment", FhirProfiles.CACORE_APPOINTMENT);
        // No Canadian guide profiles these two, so none is claimed.
        supportedResource(rest, "Schedule", null);
        supportedResource(rest, "Slot", null);

        rest.addOperation()
                .setName("summary")
                .setDefinition("http://hl7.org/fhir/uv/ips/OperationDefinition/summary");

        return fhirResponse(statement);
    }

    private void supportedResource(CapabilityStatement.CapabilityStatementRestComponent rest,
                                   String type, String profile, String... alsoSupported) {
        var resource = rest.addResource()
                .setType(type)
                .setProfile(profile);
        for (String supported : alsoSupported) {
            resource.addSupportedProfile(supported);
        }
        resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
    }

    // ---- patient --------------------------------------------------------

    @GetMapping("/Patient/{id}")
    public ResponseEntity<String> patient(@PathVariable UUID id, HttpServletRequest request) {
        return fhirResponse(mapper.toPatient(ownPatient(id, request)));
    }

    /**
     * The patient summary, as a PS-CA document.
     *
     * Named $summary after the IPS operation of the same name, so a client
     * that knows the international guide already knows how to ask for it.
     */
    @GetMapping("/Patient/{id}/$summary")
    public ResponseEntity<String> summary(@PathVariable UUID id, HttpServletRequest request) {
        return fhirResponse(summaryService.buildSummary(ownPatient(id, request)));
    }

    // ---- directory ------------------------------------------------------
    //
    // The doctor list is the public directory the app shows before sign-in,
    // so it stays readable without a token, exactly like /api/mobile/doctors.

    @GetMapping("/Practitioner")
    public ResponseEntity<String> practitioners() {
        return fhirResponse(searchBundle(doctorRepository.findAll(), mapper::toPractitioner));
    }

    @GetMapping("/Practitioner/{id}")
    public ResponseEntity<String> practitioner(@PathVariable UUID id) {
        return doctorRepository.findById(id)
                .map(doctor -> fhirResponse(mapper.toPractitioner(doctor)))
                .orElseThrow(() -> notFound("Practitioner", id));
    }

    @GetMapping("/PractitionerRole")
    public ResponseEntity<String> practitionerRoles() {
        return fhirResponse(searchBundle(doctorRepository.findAll(), mapper::toPractitionerRole));
    }

    @GetMapping("/Organization/mediconnect")
    public ResponseEntity<String> organization() {
        return fhirResponse(mapper.organization());
    }

    // ---- scheduling -----------------------------------------------------

    @GetMapping("/Schedule")
    public ResponseEntity<String> schedules() {
        return fhirResponse(searchBundle(scheduleRepository.findAll(), mapper::toSchedule));
    }

    /**
     * Free slots, which is the only question a booking client actually asks.
     * Filtering here rather than returning everything keeps a response that
     * would otherwise run to thousands of entries usable.
     */
    @GetMapping("/Slot")
    public ResponseEntity<String> slots(@RequestParam(required = false) String status) {
        var slots = scheduleTimeRepository.findAll().stream()
                .filter(slot -> status == null
                        || status.equalsIgnoreCase(Boolean.TRUE.equals(slot.getAvailable())
                        ? "free" : "busy"))
                .toList();
        return fhirResponse(searchBundle(slots, mapper::toSlot));
    }

    /**
     * A patient's appointments. The identifier is the patient's, and the
     * token has to belong to them.
     */
    @GetMapping("/Appointment")
    public ResponseEntity<String> appointments(@RequestParam("patient") UUID patientId,
                                               HttpServletRequest request) {
        Patient patient = ownPatient(patientId, request);
        var appointments = appointmentRepository.findAllByPatient(patient);
        return fhirResponse(searchBundle(appointments, mapper::toAppointment));
    }

    // ---- plumbing -------------------------------------------------------

    /**
     * Loads a patient, but only if the bearer token belongs to them.
     *
     * The interceptor already checks a token is valid and, for the mobile
     * routes, that the email in the path matches it. FHIR addresses patients
     * by id instead, so the ownership check has to happen here — otherwise
     * any signed-in patient could read anyone's summary by changing a UUID.
     */
    private Patient ownPatient(UUID id, HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> notFound("Patient", id));

        if (!patient.getEmail().equalsIgnoreCase(email.toString())) {
            // Deliberately the same shape as a missing record: telling a
            // caller that a patient exists but is not theirs still leaks
            // that the patient exists.
            throw notFound("Patient", id);
        }
        return patient;
    }

    private <T> Bundle searchBundle(List<T> source, Function<T, ? extends Resource> toResource) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(source.size());
        for (T item : source) {
            Resource resource = toResource.apply(item);
            bundle.addEntry()
                    .setFullUrl(properties.getBaseUrl() + "/" + resource.fhirType()
                            + "/" + resource.getIdElement().getIdPart())
                    .setResource(resource);
        }
        return bundle;
    }

    private ResponseStatusException notFound(String type, UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + "/" + id + " was not found.");
    }

    private ResponseEntity<String> fhirResponse(Resource resource) {
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        return ResponseEntity.ok(parser.encodeResourceToString(resource));
    }
}
