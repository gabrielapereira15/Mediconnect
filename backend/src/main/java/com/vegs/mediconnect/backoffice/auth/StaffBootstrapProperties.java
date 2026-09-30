package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.staff.StaffRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code mediconnect.staff.bootstrap.*} settings from
 * application.yml: the first back-office account of a real deployment.
 *
 * Getters and setters only. A generated toString would print the password
 * the first time anyone logged this object.
 */
@Component
@ConfigurationProperties(prefix = "mediconnect.staff.bootstrap")
@Getter
@Setter
public class StaffBootstrapProperties {

    /** Sign-in address of the account. Blank means there is nothing to create. */
    private String email = "";

    /** Name shown in the side navigation. Falls back to the email when blank. */
    private String name = "";

    /**
     * What the account may do. The front desk unless asked otherwise: a
     * clinician can read every patient's chart, and that should be a choice
     * somebody made rather than a default they missed.
     */
    private StaffRole role = StaffRole.FRONT_DESK;

    /** Only ever hashed on the way into the database, never stored or logged. */
    private String password = "";
    /**
     * For a clinician: the id of the doctor whose agenda this login may
     * change. Blank leaves the account unlinked, which fails closed — it can
     * read charts but change nobody's availability.
     */
    private String doctorId = "";
}
