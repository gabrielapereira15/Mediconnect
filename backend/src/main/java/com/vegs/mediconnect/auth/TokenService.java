package com.vegs.mediconnect.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and validates bearer tokens.
 *
 * The token is a compact {@code payload.signature} pair: the payload carries
 * the subject and an expiry, and the signature is an HMAC-SHA256 over it. That
 * is enough to make the token unforgeable and self-expiring without pulling in
 * a JWT library or Spring Security — the admin UI in this same application is
 * intentionally left unauthenticated, and adding a security filter chain would
 * lock it down as a side effect.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = ".";

    private final AuthProperties properties;

    private byte[] key;

    private byte[] key() {
        if (key == null) {
            String secret = properties.getTokenSecret();
            if (secret == null || secret.isBlank()) {
                var random = new byte[32];
                new SecureRandom().nextBytes(random);
                key = random;
                log.warn("mediconnect.auth.token-secret is not set — generated a random one. "
                        + "Tokens will not survive a restart. Set TOKEN_SECRET in production.");
            } else {
                key = secret.getBytes(StandardCharsets.UTF_8);
            }
        }
        return key;
    }

    /** Issues a token for the given email, valid for the configured lifetime. */
    public String issue(String email) {
        long expiresAt = Instant.now()
                .plusSeconds(properties.getTokenTtlHours() * 3600L)
                .getEpochSecond();

        String payload = encode(email + SEPARATOR + expiresAt);
        return payload + SEPARATOR + sign(payload);
    }

    /**
     * Returns the email a token belongs to, or empty when it is malformed,
     * has been tampered with, or has expired.
     */
    public Optional<String> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        int split = token.lastIndexOf(SEPARATOR);
        if (split <= 0) {
            return Optional.empty();
        }

        String payload = token.substring(0, split);
        String signature = token.substring(split + 1);

        // Constant-time compare so a bad signature cannot be guessed by timing.
        if (!MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        String decoded;
        try {
            decoded = decode(payload);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        int fieldSplit = decoded.lastIndexOf(SEPARATOR);
        if (fieldSplit <= 0) {
            return Optional.empty();
        }

        String email = decoded.substring(0, fieldSplit);
        long expiresAt;
        try {
            expiresAt = Long.parseLong(decoded.substring(fieldSplit + 1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }

        return Optional.of(email);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key(), ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign token", e);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
