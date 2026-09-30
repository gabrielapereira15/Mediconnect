package com.vegs.mediconnect.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two pieces that decide whether a patient's records are reachable:
 * whether a passcode can be guessed, and whether a token can be forged.
 */
class AuthTest {

    private AuthProperties properties;
    private OtpService otpService;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.setOtpTtlMinutes(10);
        properties.setTokenTtlHours(72);
        properties.setTokenSecret("test-secret-used-only-by-this-suite");
        properties.setExposeOtp(true);

        otpService = new OtpService(properties);
        tokenService = new TokenService(properties);
    }

    // ---- passcodes ------------------------------------------------------

    @Test
    @DisplayName("a passcode is six digits and verifies once")
    void passcodeVerifiesOnce() {
        String code = otpService.issue("patient@example.com");

        assertTrue(code.matches("\\d{6}"), "expected six digits, got " + code);
        assertTrue(otpService.verify("patient@example.com", code));
        // Consumed: replaying the same code must not work.
        assertFalse(otpService.verify("patient@example.com", code));
    }

    @Test
    @DisplayName("the wrong passcode is rejected")
    void wrongPasscodeRejected() {
        String code = otpService.issue("patient@example.com");
        String wrong = code.equals("000000") ? "111111" : "000000";

        assertFalse(otpService.verify("patient@example.com", wrong));
    }

    @Test
    @DisplayName("email case and surrounding space do not matter")
    void emailIsNormalised() {
        String code = otpService.issue("Patient@Example.com");

        assertTrue(otpService.verify("  patient@example.com  ", code));
    }

    @Test
    @DisplayName("a passcode is discarded after five wrong guesses")
    void passcodeBurnsOutAfterRepeatedGuesses() {
        String code = otpService.issue("patient@example.com");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(otpService.verify("patient@example.com", "999999"));
        }

        // Even the right code is now useless: a six digit space is small
        // enough to brute force if guesses were unlimited.
        assertFalse(otpService.verify("patient@example.com", code));
    }

    @Test
    @DisplayName("an expired passcode is rejected")
    void expiredPasscodeRejected() {
        properties.setOtpTtlMinutes(-1);
        String code = otpService.issue("patient@example.com");

        assertFalse(otpService.verify("patient@example.com", code));
    }

    @Test
    @DisplayName("issuing again replaces the previous passcode")
    void reissueReplacesPrevious() {
        String first = otpService.issue("patient@example.com");
        String second = otpService.issue("patient@example.com");

        assertFalse(otpService.verify("patient@example.com", first));
        assertTrue(otpService.verify("patient@example.com", second));
    }

    @Test
    @DisplayName("asking for new passcodes does not reset the limit on guessing")
    void newCodesDoNotResetTheLimit() {
        String email = "victim@example.com";
        int tried = 0;
        // Guess wrong four times per code, asking for a new one each time,
        // which kept every code alive and used to reset the count.
        while (tried < OtpService.MAX_TRIES_PER_WINDOW) {
            otpService.issue(email);
            for (int i = 0; i < 4 && tried < OtpService.MAX_TRIES_PER_WINDOW; i++, tried++) {
                assertFalse(otpService.verify(email, "not-it"));
            }
        }

        String code = otpService.issue(email);
        assertFalse(otpService.verify(email, code), "locked: even the right code is refused for now");
        assertTrue(otpService.verify("someone.else@example.com", otpService.issue("someone.else@example.com")),
                "the lock is on that email only");
    }

    @Test
    @DisplayName("a correct passcode clears the count of tries")
    void successClearsTheCount() {
        String email = "patient@example.com";
        for (int i = 0; i < OtpService.MAX_TRIES_PER_WINDOW - 1; i++) {
            otpService.issue(email);
            assertFalse(otpService.verify(email, "not-it"));
        }
        assertTrue(otpService.verify(email, otpService.issue(email)));
        for (int i = 0; i < OtpService.MAX_TRIES_PER_WINDOW - 1; i++) {
            assertFalse(otpService.verify(email, "not-it"));
        }
        assertTrue(otpService.verify(email, otpService.issue(email)), "a fresh count after signing in");
    }

    @Test
    @DisplayName("a burst of parallel guesses cannot get past five on one code")
    void parallelGuessesAreCounted() throws Exception {
        String email = "patient@example.com";
        String code = otpService.issue(email);
        String wrong = code.equals("000000") ? "000001" : "000000";

        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var guesses = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 40; i++) {
                guesses.add(pool.submit(() -> {
                    start.await();
                    return otpService.verify(email, wrong);
                }));
            }
            start.countDown();
            for (var guess : guesses) {
                assertFalse(guess.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertFalse(otpService.verify(email, code), "the code was spent after five");
    }

    @Test
    @DisplayName("guesses with no passcode pending neither count nor lock the patient out")
    void guessesWithoutACodeDoNotLock() {
        String email = "patient@example.com";
        for (int i = 0; i < OtpService.MAX_TRIES_PER_WINDOW * 3; i++) {
            assertFalse(otpService.verify(email, "000000"));
        }
        assertTrue(otpService.verify(email, otpService.issue(email)),
                "a stranger who knows the address cannot lock it without asking for a code");
    }

    // ---- tokens ---------------------------------------------------------

    @Test
    @DisplayName("a token round-trips to the email it was issued for")
    void tokenRoundTrips() {
        String token = tokenService.issue("patient@example.com");

        assertEquals(Optional.of("patient@example.com"), tokenService.verify(token));
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedTokenRejected() {
        String token = tokenService.issue("patient@example.com");

        assertTrue(tokenService.verify(token + "x").isEmpty());
        assertTrue(tokenService.verify(token.substring(0, token.length() - 1)).isEmpty());
        assertTrue(tokenService.verify("not-a-token").isEmpty());
        assertTrue(tokenService.verify("").isEmpty());
        assertTrue(tokenService.verify(null).isEmpty());
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void tokenFromAnotherSecretRejected() {
        String token = tokenService.issue("patient@example.com");

        var otherProperties = new AuthProperties();
        otherProperties.setTokenSecret("a-completely-different-secret");
        otherProperties.setTokenTtlHours(72);
        var otherService = new TokenService(otherProperties);

        assertTrue(otherService.verify(token).isEmpty(),
                "a token must not validate against a different signing key");
    }

    @Test
    @DisplayName("swapping the payload for another email is rejected")
    void payloadCannotBeSwapped() {
        String mine = tokenService.issue("patient@example.com");
        String theirs = tokenService.issue("someone.else@example.com");

        // Their payload with my signature, and vice versa.
        String myPayload = mine.substring(0, mine.lastIndexOf('.'));
        String theirSignature = theirs.substring(theirs.lastIndexOf('.') + 1);

        assertTrue(tokenService.verify(myPayload + "." + theirSignature).isEmpty());
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenRejected() {
        properties.setTokenTtlHours(-1);
        String token = tokenService.issue("patient@example.com");

        assertTrue(tokenService.verify(token).isEmpty());
    }

    @Test
    @DisplayName("two tokens for different patients do not collide")
    void tokensAreDistinctPerPatient() {
        String mine = tokenService.issue("patient@example.com");
        String theirs = tokenService.issue("someone.else@example.com");

        assertNotEquals(mine, theirs);
        assertEquals(Optional.of("patient@example.com"), tokenService.verify(mine));
        assertEquals(Optional.of("someone.else@example.com"), tokenService.verify(theirs));
    }
}
