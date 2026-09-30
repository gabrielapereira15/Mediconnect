package com.vegs.mediconnect.demo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds the {@code mediconnect.demo-data.*} settings from application.yml. */
@Component
@ConfigurationProperties(prefix = "mediconnect.demo-data")
@Getter
@Setter
public class DemoDataProperties {

    /**
     * Seeds doctors, schedules, patients and appointments into an empty
     * database. Matches the seeder's own condition, which is on unless
     * something turns it off.
     */
    private boolean enabled = true;

    /**
     * Also creates the two demo back-office logins, whose password is
     * printed in the README and the log.
     *
     * Kept apart from {@link #enabled} because a clinic can live with a few
     * made-up doctors in a database it is trying out, but not with a
     * clinician login anyone can look up. Off unless application.yml turns
     * it on, which it does only for the in-memory H2 setup: a misspelt key
     * then costs a developer the demo logins rather than handing a real
     * database two accounts with a public password.
     */
    private boolean staffAccounts = false;

    /** Whether the seeder will create the demo staff accounts at all. */
    public boolean seedsStaffAccounts() {
        return enabled && staffAccounts;
    }
}
