package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.datasource.staff.StaffUser;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import com.vegs.mediconnect.demo.DemoDataProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first back-office account of a real deployment from
 * {@code STAFF_BOOTSTRAP_EMAIL}, {@code _NAME}, {@code _ROLE} and
 * {@code _PASSWORD}.
 *
 * Until this existed the only code that ever wrote a staff account was the
 * demo seeder, which the production profile switches off, so a production
 * back office had a sign-in page nobody could get past.
 *
 * It only ever adds. If an account with that email is already there it is
 * left exactly as it is, so changing the password variable later does not
 * change anyone's password; nor does removing the variables remove the
 * account.
 *
 * Runs before the demo seeder, which only adds its two public logins to an
 * empty staff table. A clinic that has named its own first account is a real
 * clinic, whatever profile it happens to be running.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class StaffBootstrap implements ApplicationRunner {

    /**
     * A short password is one somebody can try their way to from the
     * sign-in page. Refusing it costs one restart; accepting it could open
     * every patient's chart.
     */
    static final int MIN_PASSWORD_LENGTH = 12;

    private final StaffBootstrapProperties properties;
    private final DemoDataProperties demoData;
    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;

    @Override
    public void run(ApplicationArguments args) {
        String email = trimmed(properties.getEmail());
        String password = properties.getPassword() == null ? "" : properties.getPassword();

        if (email.isEmpty()) {
            if (!password.isBlank()) {
                log.warn("Staff bootstrap: STAFF_BOOTSTRAP_PASSWORD is set but "
                        + "STAFF_BOOTSTRAP_EMAIL is not, so no account was created.");
            }
            warnIfNobodyCanSignIn();
            return;
        }

        if (staffUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Staff bootstrap: {} already has an account, leaving it unchanged.", email);
            return;
        }

        // Neither message says anything about the password beyond this rule,
        // not even its length: the log outlives the variable.
        if (password.isBlank()) {
            log.warn("Staff bootstrap: STAFF_BOOTSTRAP_EMAIL is set but "
                    + "STAFF_BOOTSTRAP_PASSWORD is not, so no account was created for {}.", email);
            warnIfNobodyCanSignIn();
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Staff bootstrap: STAFF_BOOTSTRAP_PASSWORD is shorter than {} characters, "
                    + "so no account was created for {}. Choose a longer one and restart.",
                    MIN_PASSWORD_LENGTH, email);
            warnIfNobodyCanSignIn();
            return;
        }

        StaffRole role = properties.getRole() == null ? StaffRole.FRONT_DESK : properties.getRole();
        String name = trimmed(properties.getName());

        var user = new StaffUser();
        user.setEmail(email);
        user.setName(name.isEmpty() ? email : name);
        user.setRole(role);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDoctorId(linkedDoctor(role));
        staffUserRepository.save(user);

        log.info("Staff bootstrap: created a {} account for {}.", role, email);
    }

    /**
     * Says so at startup when the back office will have no way in, rather
     * than leaving somebody to find out at a sign-in page that turns every
     * address away. Not when the demo seeder is about to add its own logins,
     * which it does after this runs.
     */
    private void warnIfNobodyCanSignIn() {
        if (demoData.seedsStaffAccounts() || staffUserRepository.count() > 0) {
            return;
        }
        log.warn("Staff bootstrap: there are no staff accounts, so nobody can sign in to the "
                + "back office. Set STAFF_BOOTSTRAP_EMAIL, STAFF_BOOTSTRAP_PASSWORD (at least {} "
                + "characters) and optionally STAFF_BOOTSTRAP_NAME and STAFF_BOOTSTRAP_ROLE "
                + "(FRONT_DESK or CLINICIAN), then restart.", MIN_PASSWORD_LENGTH);
    }

    /**
     * The doctor a clinician login may change the agenda of, from
     * STAFF_BOOTSTRAP_DOCTOR_ID. Anything that does not name an existing
     * doctor links nothing and says so, rather than stopping the start.
     */
    private java.util.UUID linkedDoctor(StaffRole role) {
        String raw = trimmed(properties.getDoctorId());
        if (raw.isEmpty()) {
            return null;
        }
        if (role != StaffRole.CLINICIAN) {
            log.warn("Staff bootstrap: STAFF_BOOTSTRAP_DOCTOR_ID is only used for a CLINICIAN, "
                    + "so the {} account is not linked to a doctor.", role);
            return null;
        }
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Staff bootstrap: STAFF_BOOTSTRAP_DOCTOR_ID is not a doctor id, "
                    + "so the account can change nobody's agenda.");
            return null;
        }
        if (!doctorRepository.existsById(id)) {
            log.warn("Staff bootstrap: no doctor has the id in STAFF_BOOTSTRAP_DOCTOR_ID, "
                    + "so the account can change nobody's agenda.");
            return null;
        }
        return id;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
