package com.vegs.mediconnect.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Requires a valid bearer token on the mobile API.
 *
 * Patient records, appointments and notifications are health data, so they are
 * only served to a caller holding a token — and only for their own email. The
 * doctor list stays open, since it is the public directory the app shows
 * before anyone signs in.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** Set on the request so controllers can see who is calling. */
    public static final String AUTHENTICATED_EMAIL = "authenticatedEmail";

    private static final String BEARER = "Bearer ";

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // Let CORS preflight through untouched.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Optional<String> email = tokenFrom(request).flatMap(tokenService::verify);

        if (email.isEmpty()) {
            return reject(response, HttpStatus.UNAUTHORIZED,
                    "Sign in to continue.");
        }

        // A token proves who you are; it must not let you read someone else.
        String requestedEmail = emailFromPath(request.getRequestURI());
        if (requestedEmail != null && !requestedEmail.equalsIgnoreCase(email.get())) {
            return reject(response, HttpStatus.FORBIDDEN,
                    "That record belongs to a different account.");
        }

        request.setAttribute(AUTHENTICATED_EMAIL, email.get());
        return true;
    }

    private Optional<String> tokenFrom(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER.length()).trim());
    }

    /**
     * The patient's email, wherever it appears in the path.
     *
     * This used to look only at the last segment, which was true of
     * /api/mobile/appointments/{email} and quietly false of every route that
     * puts an action after it — /api/mobile/health/{email}/{id}/stop ends in
     * "stop", so the check was skipped entirely and any signed-in patient
     * could edit another patient's record by changing the email.
     *
     * Every segment is checked instead, so adding a route with a trailing
     * action cannot silently opt out of the ownership check again.
     */
    private String emailFromPath(String uri) {
        for (String segment : uri.split("/")) {
            // Decoded first, not after: getRequestURI returns the raw path,
            // so a client that percent-encodes the @ as %40 would otherwise
            // produce a segment this never recognises as an email — and an
            // unrecognised email means no ownership check at all.
            String decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8);
            if (decoded.contains("@")) {
                return decoded;
            }
        }
        return null;
    }

    private boolean reject(HttpServletResponse response, HttpStatus status, String message)
            throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status.value(),
                "message", message));
        return false;
    }
}
