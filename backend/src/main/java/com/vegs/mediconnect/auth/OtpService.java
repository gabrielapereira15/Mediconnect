package com.vegs.mediconnect.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    private final AuthProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Otp> pending = new ConcurrentHashMap<>();

    private record Otp(String code, Instant expiresAt, int attempts) {
        boolean isExpired() {
            // Not isAfter: a code whose expiry is exactly now is already spent.
            return !Instant.now().isBefore(expiresAt);
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
     * Wrong guesses are counted, and the code is thrown away after
     * {@value #MAX_ATTEMPTS} so it cannot be brute forced — a six digit code
     * is only a million possibilities.
     */
    public boolean verify(String email, String code) {
        String key = normalise(email);
        Otp otp = pending.get(key);

        if (otp == null || otp.isExpired()) {
            pending.remove(key);
            return false;
        }

        // Constant-time compare, so a correct prefix is not measurably faster.
        boolean matches = MessageDigest.isEqual(
                otp.code().getBytes(StandardCharsets.UTF_8),
                code == null ? new byte[0] : code.trim().getBytes(StandardCharsets.UTF_8));

        if (matches) {
            pending.remove(key);
            return true;
        }

        int attempts = otp.attempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            pending.remove(key);
            log.warn("Passcode for {} discarded after {} failed attempts", email, attempts);
        } else {
            pending.put(key, new Otp(otp.code(), otp.expiresAt(), attempts));
        }

        return false;
    }

    /** Drops expired codes so the map cannot grow without bound. */
    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        pending.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
