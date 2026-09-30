package com.vegs.mediconnect.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Who the request is from, as the token says.
 *
 * Routes with the patient's email in the path are checked by
 * {@link AuthInterceptor}. Routes that take the patient from the body, or
 * that address a record by id, check here instead: the body is whatever
 * the caller chose to send, and only the token says who they are.
 */
public final class SignedIn {

    public static final String OTHER_ACCOUNT = "That record belongs to a different account.";

    private SignedIn() {
    }

    /** The token's email. Only reachable behind the interceptor, which sets it. */
    public static String email(HttpServletRequest request) {
        Object email = request.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
        return email.toString();
    }

    /**
     * The email a request body names, checked against the token. Left out,
     * it is the signed-in patient's own; anybody else's is a 403, the same
     * answer as an email in the path that is not the token's.
     */
    public static String sameAs(HttpServletRequest request, String claimed) {
        String email = email(request);
        if (claimed != null && !claimed.isBlank() && !claimed.trim().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, OTHER_ACCOUNT);
        }
        return email;
    }
}
