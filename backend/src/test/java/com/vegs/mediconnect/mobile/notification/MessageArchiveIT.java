package com.vegs.mediconnect.mobile.notification;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Archiving a message (P13): out of the inbox, never gone, and only by
 * the patient it was sent to.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class MessageArchiveIT {

    private static final String EMAIL = "demo@mediconnect.ca";
    private static final String OTHER = "john.doe@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper json;

    private JsonNode messages(String token) throws Exception {
        String body = mockMvc.perform(get("/api/mobile/notifications/" + EMAIL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode message(String token, String id) throws Exception {
        for (JsonNode message : messages(token)) {
            if (id.equals(message.path("id").asText())) {
                return message;
            }
        }
        throw new AssertionError("message " + id + " is no longer listed");
    }

    @Test
    @Transactional
    @DisplayName("archiving keeps the message, marks it read, and can be undone")
    void archiveAndBack() throws Exception {
        String token = tokenService.issue(EMAIL);
        JsonNode first = messages(token).get(0);
        String id = first.path("id").asText();
        assertFalse(first.path("archived").asBoolean(), "new messages start in the inbox");

        mockMvc.perform(post("/api/mobile/notifications/archive/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
        JsonNode archived = message(token, id);
        assertTrue(archived.path("archived").asBoolean());
        assertTrue(archived.path("read").asBoolean(), "an archived message never keeps the bell lit");

        mockMvc.perform(post("/api/mobile/notifications/unarchive/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
        JsonNode back = message(token, id);
        assertFalse(back.path("archived").asBoolean());
        assertTrue(back.path("read").asBoolean(), "back in the inbox, still read");
    }

    @Test
    @Transactional
    @DisplayName("clear read archives exactly the read ones and says which")
    void clearRead() throws Exception {
        String token = tokenService.issue(EMAIL);
        String readId = messages(token).get(0).path("id").asText();
        mockMvc.perform(post("/api/mobile/notifications/ack/" + readId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        String body = mockMvc.perform(post("/api/mobile/notifications/archive-read/" + EMAIL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ids = json.readTree(body);
        assertTrue(ids.isArray());

        int readBefore = 0;
        for (JsonNode message : messages(token)) {
            boolean listed = false;
            for (JsonNode archivedId : ids) {
                listed |= archivedId.asText().equals(message.path("id").asText());
            }
            assertEquals(listed, message.path("archived").asBoolean(),
                    "archived exactly when it came back from clear read");
            if (listed) {
                assertTrue(message.path("read").asBoolean(), "only read messages are cleared");
                readBefore++;
            } else {
                assertFalse(message.path("read").asBoolean(), "unread ones stay in the inbox");
            }
        }
        assertTrue(readBefore >= 1, "the message just read was cleared");
    }

    @Test
    @Transactional
    @DisplayName("another patient cannot archive someone else's message")
    void onlyTheOwner() throws Exception {
        String id = messages(tokenService.issue(EMAIL)).get(0).path("id").asText();
        String stranger = tokenService.issue(OTHER);

        mockMvc.perform(post("/api/mobile/notifications/archive/" + id)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/mobile/notifications/unarchive/" + id)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/mobile/notifications/archive-read/" + EMAIL)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());

        assertFalse(message(tokenService.issue(EMAIL), id).path("archived").asBoolean());
    }
}
