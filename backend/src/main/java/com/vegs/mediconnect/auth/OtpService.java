package com.vegs.mediconnect.auth;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and checks one-time passcodes.
 *
 * Codes live in memory: this is a demo clinic with no mail provider attached,
 * and a restart clearing pending codes is harmless. A production deployment
 * would persist them and send them by email or SMS instead of returning them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Passcode tries allowed per email in {@link #TRY_WINDOW}, across every
     * code issued to it. Counted apart from the codes, so asking for a new
     * one does not start the count again.
     */
    static final int MAX_TRIES_PER_WINDOW = 10;
    static final Duration TRY_WINDOW = Duration.ofHours(24);

    /** The value application.yml falls back to outside production. Published, so never safe. */
    static final String DEVELOPMENT_SECRET = "mediconnect-local-development-secret-not-for-production";

    private final AuthProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Otp> pending = new ConcurrentHashMap<>();
    private final Map<String, Tries> tries = new ConcurrentHashMap<>();

    private record Otp(String code, Instant expiresAt, int attempts) {
        boolean isExpired() {
            // Not isAfter: a code whose expiry is exactly now is already spent.
            return !Instant.now().isBefore(expiresAt);
        }
    }

    /** How many passcode tries an email has had since {@code since}. */
    private record Tries(int count, Instant since) {
        boolean isCurrent(Instant now) {
            return now.isBefore(since.plus(TRY_WINDOW));
        }
    }

    /**
     * Says so at startup when anyone who can reach the server can sign in as
     * any patient: the passcode is in the reply, or tokens are signed with a
     * secret published in the repository. Both are right for a demo on a
     * laptop, where there is no mail provider to send a code with, and wrong
     * anywhere else. The production profile turns both off.
     */
    @PostConstruct
    void warnIfDevelopmentOnly() {
        if (properties.isExposeOtp()) {
            log.warn("Passcodes are returned by /auth/get-otp and written to this log "
                    + "(mediconnect.auth.expose-otp). Development only: anyone who can reach "
                    + "this server can sign in as any patient. Use the production profile to deploy.");
        }
        if (DEVELOPMENT_SECRET.equals(properties.getTokenSecret())) {
            log.warn("Tokens are signed with the development secret published in application.yml. "
                    + "Development only: set TOKEN_SECRET to deploy.");
        }
    }

    /** Creates a passcode for an email, replacing any code already pending. */
    public String issue(String email) {
        String code = String.format("%0" + CODE_LENGTH + "d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(properties.getOtpTtlMinutes() * 60L);

        pending.put(normalise(email), new Otp(code, expiresAt, 0));

        if (properties.isExposeOtp()) {
            log.info("Passcode for {} is {} (valid {} minutes)",
                    email, code, properties.getOtpTtlMinutes());
        }

        return code;
    }

    /**
     * Checks a passcode and consumes it on success.
     *
     * A six digit code is only a million possibilities, so guessing is
     * limited twice over. A code is thrown away after {@value #MAX_ATTEMPTS}
     * wrong guesses. And an email gets {@value #MAX_TRIES_PER_WINDOW} tries
     * a day in all, however many codes are asked for: the limit used to live
     * on the code alone, so asking for a new one reset it and anybody could
     * keep guessing until they had a token for somebody else.
     *
     * Each try is counted before the code is looked at, and the code is
     * checked and spent in one step, so a burst of parallel guesses cannot
     * get past either limit. A lock ends when the day since the first try
     * is up; a correct passcode clears the count.
     */
    public boolean verify(String email, String code) {
        String key = normalise(email);
        Instant now = Instant.now();

        Tries sofar = tries.compute(key, (k, t) -> t == null || !t.isCurrent(now)
                ? new Tries(1, now)
                : new Tries(t.count() + 1, t.since()));
        if (sofar.count() > MAX_TRIES_PER_WINDOW) {
            if (sofar.count() == MAX_TRIES_PER_WINDOW + 1) {
                log.warn("Sign-in for {} refused for up to {} hours after {} passcode tries",
                        email, TRY_WINDOW.toHours(), MAX_TRIES_PER_WINDOW);
            }
            return false;
        }

        byte[] given = code == null ? new byte[0] : code.trim().getBytes(StandardCharsets.UTF_8);
        boolean[] matched = {false};
        pending.compute(key, (k, otp) -> {
            if (otp == null || otp.isExpired()) {
                return null;
            }
            // Constant-time compare, so a correct prefix is not measurably faster.
            if (MessageDigest.isEqual(otp.code().getBytes(StandardCharsets.UTF_8), given)) {
                matched[0] = true;
                return null;
            }
            int attempts = otp.attempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                log.warn("Passcode for {} discarded after {} failed attempts", email, attempts);
                return null;
            }
            return new Otp(otp.code(), otp.expiresAt(), attempts);
        });

        if (matched[0]) {
            tries.remove(key);
            return true;
        }
        return false;
    }

    /** Drops expired codes so the map cannot grow without bound. */
    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        pending.entrySet().removeIf(entry -> entry.getValue().isExpired());
        Instant now = Instant.now();
        tries.entrySet().removeIf(entry -> !entry.getValue().isCurrent(now));
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
