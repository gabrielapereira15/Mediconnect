package com.vegs.mediconnect.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds the {@code mediconnect.auth.*} settings from application.yml. */
@Component
@ConfigurationProperties(prefix = "mediconnect.auth")
@Getter
@Setter
public class AuthProperties {

    /** How long a one-time passcode stays valid. */
    private int otpTtlMinutes = 10;

    /** How long a token issued after verification stays valid. */
    private int tokenTtlHours = 72;

    /**
     * HMAC signing secret. Left blank a random one is generated at startup,
     * which is fine for local use but invalidates every token on restart.
     * Set TOKEN_SECRET in production.
     */
    private String tokenSecret = "";

    /**
     * Returns the passcode in the API response and logs it, because no mail
     * provider is wired up. Must be false in production.
     */
    private boolean exposeOtp = true;
}
