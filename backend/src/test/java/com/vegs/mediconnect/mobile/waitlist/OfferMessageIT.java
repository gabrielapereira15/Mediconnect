package com.vegs.mediconnect.mobile.waitlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegs.mediconnect.auth.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A waitlist offer's message says whether the offer is still there.
 *
 * The message stays in the inbox after the offer ends, so without this
 * the app kept showing "See offer" for a slot that had gone to someone
 * else.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class OfferMessageIT {

    private static final String EMAIL = "demo@mediconnect.ca";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper json;

    private JsonNode read(String path, String token) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode offerMessage(String token) throws Exception {
        for (JsonNode message : read("/api/mobile/notifications/" + EMAIL, token)) {
            if ("WAITLIST_OFFER".equals(message.path("kind").asText())) {
                return message;
            }
        }
        return null;
    }

    @Test
    @Transactional
    @DisplayName("the offer message is open while the slot is held, and ended once it is passed on")
    void endsWithTheOffer() throws Exception {
        String token = tokenService.issue(EMAIL);

        JsonNode before = offerMessage(token);
        assertNotNull(before, "the demo seed holds a slot for the demo patient");
        assertTrue(before.path("offerOpen").asBoolean(), "held right now, so the app offers See offer");

        String entryId = null;
        for (JsonNode entry : read("/api/mobile/waitlist/" + EMAIL, token)) {
            if ("OFFERED".equals(entry.path("status").asText())) {
                entryId = entry.path("id").asText();
            }
        }
        assertNotNull(entryId);
        mockMvc.perform(post("/api/mobile/waitlist/" + EMAIL + "/" + entryId + "/decline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertFalse(offerMessage(token).path("offerOpen").asBoolean(),
                "passed on, so the message says the offer has ended");
    }
}
