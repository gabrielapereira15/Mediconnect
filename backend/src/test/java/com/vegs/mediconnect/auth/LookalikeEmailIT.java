package com.vegs.mediconnect.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An address that only looks like a patient's cannot be used to reach them.
 *
 * Java's case-insensitive compare treats the dotless i (U+0131), the dotted
 * capital I (U+0130) and the long s (U+017F) as i and s, while lower-casing
 * keeps them apart. A lookalike used to sign in under its own passcode
 * budget and come back with a token that every ownership check accepted
 * for the real patient.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class LookalikeEmailIT {

    private static final String REAL = "demo@mediconnect.ca";
    private static final String LOOKALIKE = "demo@medıconnect.ca";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("sign-in refuses an address that is not plain ASCII")
    void signInRefusesLookalikes() throws Exception {
        mockMvc.perform(post("/auth/get-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + LOOKALIKE + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + LOOKALIKE + "\",\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/get-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + REAL + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a token for a lookalike address opens nothing, not even the real patient's record")
    void lookalikeTokenIsRefused() throws Exception {
        String token = tokenService.issue(LOOKALIKE);

        mockMvc.perform(get("/api/mobile/patients/" + REAL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/mobile/appointments/" + REAL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
