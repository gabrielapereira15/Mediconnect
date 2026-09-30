package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Signing in to a production back office.
 *
 * The production profile switches the demo seeder off, and the seeder used
 * to be the only thing that ever created a staff account, so this is the
 * setup in which nobody could sign in at all. It runs the real profile with
 * the real variable names; only the database is swapped for an in-memory
 * one of its own, so it neither needs Postgres nor sees the demo accounts
 * the other tests' database has.
 */
@SpringBootTest(properties = {
        "JDBC_DATABASE_URL=jdbc:h2:mem:staff_bootstrap_it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "JDBC_DATABASE_USERNAME=sa",
        "JDBC_DATABASE_PASSWORD=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "TOKEN_SECRET=staff-bootstrap-it-secret",
        "STAFF_BOOTSTRAP_EMAIL=" + StaffBootstrapIT.EMAIL,
        "STAFF_BOOTSTRAP_NAME=Jordan Lee",
        // Lower case on purpose: that is how people type it into a
        // deployment's settings, and it should still bind.
        "STAFF_BOOTSTRAP_ROLE=clinician",
        "STAFF_BOOTSTRAP_PASSWORD=" + StaffBootstrapIT.PASSWORD
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
// A context of its own that nothing else shares, so it is closed rather than
// kept alive for the rest of the run.
@DirtiesContext
class StaffBootstrapIT {

    static final String EMAIL = "lead@clinic.example";
    static final String PASSWORD = "correct horse battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffUserRepository staffUsers;

    @Test
    @DisplayName("the account named in the environment is created with its role, and nothing else")
    void createsOnlyTheConfiguredAccount() {
        var user = staffUsers.findByEmailIgnoreCase(EMAIL).orElseThrow();

        assertEquals(StaffRole.CLINICIAN, user.getRole());
        assertEquals("Jordan Lee", user.getName());
        assertEquals(1, staffUsers.count(), "production must not get the demo logins");
        assertTrue(staffUsers.findByEmailIgnoreCase("desk@mediconnect.ca").isEmpty());
        assertTrue(staffUsers.findByEmailIgnoreCase("doctor@mediconnect.ca").isEmpty());
    }

    @Test
    @DisplayName("that account can sign in to the back office")
    void theConfiguredAccountCanSignIn() throws Exception {
        mockMvc.perform(post("/staff/login")
                        .param("email", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @DisplayName("the demo password does not open anything")
    void theDemoLoginsDoNotExist() throws Exception {
        mockMvc.perform(post("/staff/login")
                        .param("email", "doctor@mediconnect.ca")
                        .param("password", "demo"))
                .andExpect(status().isOk());
    }
}
