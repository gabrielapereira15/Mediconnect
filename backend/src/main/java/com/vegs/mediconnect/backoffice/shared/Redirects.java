package com.vegs.mediconnect.backoffice.shared;

/**
 * Where a back-office form may send someone afterwards.
 *
 * Forms carry the page they came from so an action on the Today board
 * lands back on Today and the same action on Appointments lands back on
 * Appointments. That address comes from the request, so it is only ever
 * followed within this application: an absolute one would make any form an
 * open redirect. A backslash is refused too, since browsers read "/\host"
 * as "//host".
 */
public final class Redirects {

    private Redirects() {
    }

    public static String within(String target, String fallback) {
        if (target == null || target.isBlank()
                || !target.startsWith("/")
                || target.startsWith("//")
                || target.contains("\\")
                || target.contains("\r")
                || target.contains("\n")) {
            return fallback;
        }
        return target;
    }
}
