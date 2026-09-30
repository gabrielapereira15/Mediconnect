package com.vegs.mediconnect.backoffice.auth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.datasource.staff.StaffUser;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import com.vegs.mediconnect.demo.DemoDataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The first back-office account of a real deployment.
 *
 * Without it a production back office has a sign-in page and nobody who
 * can get past it, so what matters is that it creates the account it was
 * asked for, exactly once, with the role it was given, and refuses a
 * password that would make that account the easy way in.
 */
class StaffBootstrapTest {

    private static final String EMAIL = "lead@clinic.example";
    private static final String PASSWORD = "correct horse battery";

    /** Stands in for the staff table, so a second start sees the first one's row. */
    private final List<StaffUser> table = new ArrayList<>();

    private final StaffUserRepository repository = mock(StaffUserRepository.class);
    // The lowest cost BCrypt allows: these tests check what is stored, not how slowly.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final StaffBootstrapProperties properties = new StaffBootstrapProperties();
    private final DemoDataProperties demoData = new DemoDataProperties();

    @BeforeEach
    void fakeTheTable() {
        when(repository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return table.stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        });
        when(repository.save(any(StaffUser.class))).thenAnswer(invocation -> {
            StaffUser user = invocation.getArgument(0);
            table.add(user);
            return user;
        });
        when(repository.count()).thenAnswer(invocation -> (long) table.size());
    }

    private void start() {
        new StaffBootstrap(properties, demoData, repository, encoder)
                .run(new DefaultApplicationArguments());
    }

    private void configure(String email, String password) {
        properties.setEmail(email);
        properties.setPassword(password);
    }

    @Test
    @DisplayName("creates the account, with the password hashed and nothing else stored")
    void createsTheAccount() {
        configure(EMAIL, PASSWORD);
        properties.setName("Jordan Lee");

        start();

        assertEquals(1, table.size());
        StaffUser user = table.get(0);
        assertEquals(EMAIL, user.getEmail());
        assertEquals("Jordan Lee", user.getName());
        assertTrue(user.getActive());
        assertNotEquals(PASSWORD, user.getPasswordHash(), "the password itself must never be stored");
        assertTrue(encoder.matches(PASSWORD, user.getPasswordHash()),
                "the stored hash should accept the configured password at the sign-in page");
    }

    @Test
    @DisplayName("creates the account once, and a later start leaves it as it is")
    void createsTheAccountOnce() {
        configure(EMAIL, PASSWORD);
        start();

        // Every restart runs this again with the same variables, and a
        // changed variable must not quietly reset somebody's password.
        properties.setPassword("a different long password");
        properties.setEmail(EMAIL.toUpperCase());
        start();

        verify(repository, times(1)).save(any(StaffUser.class));
        assertEquals(1, table.size());
        assertTrue(encoder.matches(PASSWORD, table.get(0).getPasswordHash()));
    }

    @Test
    @DisplayName("gives the account the role it was asked for")
    void respectsTheRole() {
        configure(EMAIL, PASSWORD);
        properties.setRole(StaffRole.CLINICIAN);

        start();

        assertEquals(StaffRole.CLINICIAN, table.get(0).getRole());
    }

    @Test
    @DisplayName("without a role, the account is the front desk rather than a clinician")
    void defaultsToTheFrontDesk() {
        configure(EMAIL, PASSWORD);
        properties.setRole(null);

        start();

        assertEquals(StaffRole.FRONT_DESK, table.get(0).getRole());
    }

    @Test
    @DisplayName("without a name, the account is named by its email")
    void namesTheAccountByItsEmail() {
        configure("  " + EMAIL + "  ", PASSWORD);

        start();

        assertEquals(EMAIL, table.get(0).getEmail());
        assertEquals(EMAIL, table.get(0).getName());
    }

    @Test
    @DisplayName("a password shorter than 12 characters creates nothing")
    void refusesAShortPassword() {
        configure(EMAIL, "elevenchars");

        start();

        verify(repository, never()).save(any(StaffUser.class));
        assertTrue(table.isEmpty());
    }

    @Test
    @DisplayName("a password of exactly 12 characters is long enough")
    void acceptsTwelveCharacters() {
        configure(EMAIL, "twelve chars");

        start();

        assertEquals(1, table.size());
    }

    @Test
    @DisplayName("an email without a password, or a password without an email, creates nothing")
    void needsBoth() {
        configure(EMAIL, "");
        start();
        configure("", PASSWORD);
        start();
        configure(null, null);
        start();

        verify(repository, never()).save(any(StaffUser.class));
    }

    @Test
    @DisplayName("the password never reaches the log, whether it is used or refused")
    void neverLogsThePassword() {
        var logger = (Logger) LoggerFactory.getLogger(StaffBootstrap.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            configure("short@clinic.example", "too short");
            start();
            configure(EMAIL, PASSWORD);
            start();
        } finally {
            logger.detachAppender(appender);
        }

        assertFalse(appender.list.isEmpty(), "both outcomes should be reported at startup");
        for (ILoggingEvent event : appender.list) {
            String message = event.getFormattedMessage();
            assertFalse(message.contains(PASSWORD), message);
            assertFalse(message.contains("too short"), message);
        }
    }
}
