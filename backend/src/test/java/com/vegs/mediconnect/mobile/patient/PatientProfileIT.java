package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.auth.TokenService;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edit profile's save, driven through the running application.
 *
 * The unit test covers the rules; this covers what the phone actually
 * sends. The body below is the one the app built before it had a card
 * section, and saving it used to leave the seeded card as null.
 *
 * Runs against the demo profile, whose patients are seeded with a card.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PatientProfileIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EntityManager entityManager;

    private Patient demoPatient() {
        return patientRepository.findAll().getFirst();
    }

    /** Every required field, as Edit profile fills them in. */
    private String profileBody(String email, String cardFields) {
        return """
                {
                  "email": "%s",
                  "clinicCode": "KIT001",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "Female",
                  "birthdate": "1990-04-12",
                  "phoneNumber": "519-555-0100",
                  "address": "15 Wellington Street, Kitchener, ON"%s
                }
                """.formatted(email, cardFields);
    }

    @Test
    @Transactional
    @DisplayName("saving a profile without card fields keeps the stored card")
    void profileSaveKeepsTheCard() throws Exception {
        Patient patient = demoPatient();
        String number = patient.getHealthCardNumber();
        String province = patient.getHealthCardProvince();
        assertNotNull(number, "the demo patients are seeded with a card");

        mockMvc.perform(post("/api/mobile/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issue(patient.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(patient.getEmail(), "")))
                .andExpect(status().isCreated());

        // Read back from the database, not from the objects this test holds.
        entityManager.flush();
        entityManager.clear();
        Patient after = patientRepository.findByEmail(patient.getEmail()).orElseThrow();
        assertEquals(number, after.getHealthCardNumber());
        assertEquals(province, after.getHealthCardProvince());
        assertEquals("Jane", after.getFirstName());
    }

    @Test
    @Transactional
    @DisplayName("a province that is not Canadian comes back as a 400 that says why")
    void unknownProvinceIsRefused() throws Exception {
        Patient patient = demoPatient();
        String number = patient.getHealthCardNumber();

        mockMvc.perform(post("/api/mobile/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issue(patient.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(patient.getEmail(),
                                ",\n  \"healthCardNumber\": \"9876543210\",\n  \"healthCardProvince\": \"NY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("must be one of AB, BC")));

        entityManager.clear();
        assertEquals(number,
                patientRepository.findByEmail(patient.getEmail()).orElseThrow().getHealthCardNumber());
    }
}
