package com.vegs.mediconnect.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Whether a token can reach another patient's records.
 *
 * The interceptor used to look for the patient's email only in the last
 * path segment. That happened to be true of the routes which existed when
 * it was written, and quietly false of every route added since that puts an
 * action after the email — /api/mobile/health/{email}/{id}/stop ends in
 * "stop", so the ownership check was skipped and any signed-in patient
 * could edit somebody else's health record.
 *
 * These pin the behaviour down so the next route with a trailing action
 * cannot opt out of it by accident.
 */
class AuthPathOwnershipTest {

    private static final String OWNER = "owner@example.com";
    private static final String STRANGER = "stranger@example.com";

    private AuthInterceptor interceptor;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        var properties = new AuthProperties();
        properties.setTokenTtlHours(72);
        properties.setTokenSecret("test-secret-used-only-by-this-suite");

        var tokenService = new TokenService(properties);
        ownerToken = tokenService.issue(OWNER);

        interceptor = new AuthInterceptor(tokenService, new ObjectMapper());
    }

    private boolean allows(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken);
        HttpServletResponse response = new MockHttpServletResponse();
        return interceptor.preHandle(request, response, new Object());
    }

    @Test
    @DisplayName("a patient reaches their own records")
    void ownRecordsAreAllowed() throws Exception {
        assertTrue(allows("/api/mobile/health/" + OWNER));
        assertTrue(allows("/api/mobile/appointments/" + OWNER));
    }

    @Test
    @DisplayName("a patient is refused someone else's records")
    void otherRecordsAreRefused() throws Exception {
        assertFalse(allows("/api/mobile/health/" + STRANGER));
        assertFalse(allows("/api/mobile/appointments/" + STRANGER));
    }

    @Test
    @DisplayName("an action after the email does not skip the check")
    void trailingActionStillChecksOwnership() throws Exception {
        String entryId = "11111111-1111-1111-1111-111111111111";

        assertTrue(allows("/api/mobile/health/" + OWNER + "/" + entryId + "/stop"),
                "the owner should still be able to act on their own entry");

        assertFalse(allows("/api/mobile/health/" + STRANGER + "/" + entryId + "/stop"),
                "this is the hole: the last segment is \"stop\", so the email "
                        + "was never compared against the token");
        assertFalse(allows("/api/mobile/waitlist/" + STRANGER + "/" + entryId + "/leave"));
    }

    @Test
    @DisplayName("a url-encoded email is still recognised")
    void encodedEmailIsChecked() throws Exception {
        // An @ in a path is legal but some clients percent-encode it, and an
        // unrecognised email means no check at all.
        assertFalse(allows("/api/mobile/health/" + STRANGER.replace("@", "%40")));
    }

    @Test
    @DisplayName("no token is refused outright")
    void missingTokenIsRefused() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mobile/health/" + OWNER);
        request.setRequestURI("/api/mobile/health/" + OWNER);
        HttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
    }
}
