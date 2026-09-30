package com.vegs.mediconnect.mobile.waitlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegs.mediconnect.auth.TokenService;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profile's "Earlier-slot offers" switch: on by default, off pauses the
 * offers without leaving any list, and only the patient can flip it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class OffersSwitchIT {

    private static final String EMAIL = "demo@mediconnect.ca";
    private static final String OFFERS = "/api/mobile/waitlist/" + EMAIL + "/offers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private WaitlistEntryRepository waitlist;

    private boolean on(String token) throws Exception {
        String body = mockMvc.perform(get(OFFERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("on").asBoolean();
    }

    private void set(String token, boolean on) throws Exception {
        mockMvc.perform(put(OFFERS).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\":" + on + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("on by default, and off and on again from the app")
    void flips() throws Exception {
        String token = tokenService.issue(EMAIL);
        int lists = waitlist.findAllByPatientOrderByDateCreatedDesc(patients.findByEmail(EMAIL).orElseThrow()).size();

        assertTrue(on(token), "offers were always on before there was a switch");
        set(token, false);
        assertFalse(on(token));
        assertTrue(lists == waitlist.findAllByPatientOrderByDateCreatedDesc(
                patients.findByEmail(EMAIL).orElseThrow()).size(), "pausing leaves no list");
        set(token, true);
        assertTrue(on(token));
    }

    @Test
    @Transactional
    @DisplayName("asking for an earlier time turns paused offers back on")
    void joiningTurnsItOn() throws Exception {
        String token = tokenService.issue(EMAIL);
        set(token, false);

        var patient = patients.findByEmail(EMAIL).orElseThrow();
        var doctor = doctors.findAll().stream()
                .filter(candidate -> waitlist.findAllByPatientOrderByDateCreatedDesc(patient).stream()
                        .noneMatch(entry -> entry.getDoctor().getId().equals(candidate.getId())))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/mobile/waitlist/" + EMAIL).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":\"" + doctor.getId() + "\",\"currentAppointmentDate\":\""
                                + LocalDate.now().plusDays(20) + "\"}"))
                .andExpect(status().isOk());

        assertTrue(on(token));
    }

    @Test
    @Transactional
    @DisplayName("nobody else can read or flip a patient's switch, and a blank body is refused")
    void onlyThePatient() throws Exception {
        String stranger = tokenService.issue("john.doe@example.com");
        mockMvc.perform(get(OFFERS).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(OFFERS).header("Authorization", "Bearer " + stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"on\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(OFFERS).header("Authorization", "Bearer " + tokenService.issue(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
