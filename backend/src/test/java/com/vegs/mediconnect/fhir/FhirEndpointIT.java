package com.vegs.mediconnect.fhir;

import com.vegs.mediconnect.auth.OtpService;
import com.vegs.mediconnect.auth.TokenService;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Drives the FHIR endpoints through the running application.
 *
 * The unit tests validate resources the mapper builds in memory, which says
 * nothing about whether the endpoints can build them at all. They could
 * not: mapping an appointment walks scheduleTime -> schedule -> doctor, all
 * lazy, and without a transaction open the first traversal threw and the
 * endpoint returned a 500 while every unit test stayed green.
 *
 * Runs against the demo profile, so there is seeded data to serve.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class FhirEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OtpService otpService;

    private String tokenForDemoPatient() {
        var patient = patientRepository.findAll().getFirst();
        return tokenService.issue(patient.getEmail());
    }

    private String demoPatientId() {
        return patientRepository.findAll().getFirst().getId().toString();
    }

    @Test
    @DisplayName("the capability statement is public and names CA Core+")
    void metadataIsPublic() throws Exception {
        mockMvc.perform(get("/fhir/metadata"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("patient-ca-core")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4.0.1")));
    }

    @Test
    @DisplayName("the practitioner directory is public")
    void practitionersArePublic() throws Exception {
        mockMvc.perform(get("/fhir/Practitioner"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("practitioner-ca-core")));
    }

    @Test
    @DisplayName("slots are served without blowing up on lazy associations")
    void slotsAreServed() throws Exception {
        mockMvc.perform(get("/fhir/Slot").param("status", "free"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceType\": \"Slot\"")));
    }

    @Test
    @DisplayName("a patient reads their own record")
    void patientReadsOwnRecord() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + demoPatientId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForDemoPatient()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("patient-ca-core")));
    }

    @Test
    @DisplayName("appointments are served — the endpoint that used to 500")
    void appointmentsAreServed() throws Exception {
        mockMvc.perform(get("/fhir/Appointment")
                        .param("patient", demoPatientId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForDemoPatient()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("appointment-ca-core")));
    }

    @Test
    @DisplayName("the patient summary is served as a PS-CA document")
    void summaryIsServed() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + demoPatientId() + "/$summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForDemoPatient()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bundle-ca-ps")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\": \"document\"")));
    }

    @Test
    @DisplayName("health data is refused without a token")
    void healthDataNeedsAToken() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + demoPatientId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/fhir/Patient/" + demoPatientId() + "/$summary"))
                .andExpect(status().isUnauthorized());
    }
}
