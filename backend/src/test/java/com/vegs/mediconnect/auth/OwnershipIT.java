package com.vegs.mediconnect.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.schedule.BookingRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The routes that take the patient from the body, or address a record by
 * id, act only for the patient the token names.
 *
 * They were the four the interceptor could not check, because there is no
 * email in their path: booking, saving a profile, updating a profile by id,
 * and posting a review.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class OwnershipIT {

    private static final String ME = "demo@mediconnect.ca";
    private static final String SOMEONE_ELSE = "john.doe@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private AppointmentRepository appointments;

    @Autowired
    private ScheduleTimeRepository slots;

    @Autowired
    private ReviewRepository reviews;

    private MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.header("Authorization", "Bearer " + tokenService.issue(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
    }

    private Patient patient(String email) {
        return patients.findByEmail(email).orElseThrow();
    }

    private ScheduleTime freeSlot() {
        return slots.findAllBetween(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)).stream()
                .filter(BookingRules::isBookable)
                .filter(slot -> !Boolean.TRUE.equals(slot.getBlocked()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the demo seed has free times ahead"));
    }

    private Map<String, Object> profile(String email, String firstName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("clinicCode", "KIT001");
        body.put("firstName", firstName);
        body.put("lastName", "Changed");
        body.put("gender", "Female");
        body.put("birthdate", "1990-01-01");
        body.put("phoneNumber", "519-000-0000");
        body.put("address", "1 Somewhere Street");
        return body;
    }

    private Appointment pastVisitWithoutReview(String email) {
        return appointments.findAllByPatient(patient(email)).stream()
                .filter(visit -> !Boolean.TRUE.equals(visit.getCanceled()))
                .filter(visit -> visit.getDateTime().isBefore(LocalDateTime.now()))
                .filter(visit -> reviews.findByAppointment(visit).isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the demo seed has a past visit left to review"));
    }

    // ---- POST /api/mobile/appointments ------------------------------------------

    @Test
    @DisplayName("booking in someone else's name is refused, and the time stays free")
    void cannotBookForSomeoneElse() throws Exception {
        ScheduleTime slot = freeSlot();

        mockMvc.perform(as(ME, post("/api/mobile/appointments"),
                        Map.of("patientEmail", SOMEONE_ELSE, "scheduleTimeId", slot.getId())))
                .andExpect(status().isForbidden());

        assertTrue(slots.findById(slot.getId()).orElseThrow().getAvailable());
    }

    @Test
    @DisplayName("a booking without an email is the signed-in patient's own")
    void booksForTheToken() throws Exception {
        ScheduleTime slot = freeSlot();

        mockMvc.perform(as(ME, post("/api/mobile/appointments"), Map.of("scheduleTimeId", slot.getId())))
                .andExpect(status().isCreated());

        assertTrue(appointments.findAllByPatient(patient(ME)).stream()
                .anyMatch(visit -> visit.getScheduleTime().getId().equals(slot.getId())));
    }

    // ---- POST and PUT /api/mobile/patients ---------------------------------------

    @Test
    @DisplayName("saving a profile under someone else's email is refused")
    void cannotSaveSomeoneElsesProfile() throws Exception {
        String before = patient(SOMEONE_ELSE).getFirstName();

        mockMvc.perform(as(ME, post("/api/mobile/patients"), profile(SOMEONE_ELSE, "Mallory")))
                .andExpect(status().isForbidden());

        assertEquals(before, patient(SOMEONE_ELSE).getFirstName());
    }

    @Test
    @DisplayName("updating someone else's profile by id is reported as missing")
    void cannotUpdateSomeoneElsesProfile() throws Exception {
        Patient other = patient(SOMEONE_ELSE);
        String before = other.getFirstName();

        mockMvc.perform(as(ME, put("/api/mobile/patients/" + other.getId()), profile(SOMEONE_ELSE, "Mallory")))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(ME, put("/api/mobile/patients/" + UUID.randomUUID()), profile(ME, "Nobody")))
                .andExpect(status().isNotFound());

        assertEquals(before, patient(SOMEONE_ELSE).getFirstName());
    }

    @Test
    @DisplayName("updating your own profile works, but does not change the email you sign in with")
    void updatesOwnProfileButNotTheSignInEmail() throws Exception {
        Patient me = patient(ME);

        mockMvc.perform(as(ME, put("/api/mobile/patients/" + me.getId()), profile("someone.new@example.com", "Gabi")))
                .andExpect(status().isOk());

        Patient after = patients.findById(me.getId()).orElseThrow();
        assertEquals(ME, after.getEmail());
        assertEquals("Gabi", after.getFirstName());
        assertFalse(patients.findByEmail("someone.new@example.com").isPresent());
    }

    // ---- POST /api/mobile/reviews -------------------------------------------------

    @Test
    @DisplayName("reviewing someone else's visit is reported as missing")
    void cannotReviewSomeoneElsesVisit() throws Exception {
        Appointment theirs = appointments.findAllByPatient(patient(SOMEONE_ELSE)).get(0);

        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", theirs.getId(), "score", 1)))
                .andExpect(status().isNotFound());

        assertTrue(reviews.findByAppointment(theirs).isEmpty());
    }

    @Test
    @DisplayName("a visit is reviewed once it has happened, and only once")
    void reviewsOwnPastVisitOnce() throws Exception {
        Appointment past = pastVisitWithoutReview(ME);

        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", past.getId(), "score", 4, "description", "Kind and on time.")))
                .andExpect(status().isCreated());
        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", past.getId(), "score", 5)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a visit still ahead cannot be reviewed")
    void cannotReviewAVisitAhead() throws Exception {
        Appointment ahead = appointments.findAllByPatient(patient(ME)).stream()
                .filter(visit -> !Boolean.TRUE.equals(visit.getCanceled()))
                .filter(visit -> visit.getDateTime().isAfter(LocalDateTime.now()))
                .findFirst().orElseThrow();

        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", ahead.getId(), "score", 5)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a score outside one to five stars is refused")
    void scoreMustBeOneToFive() throws Exception {
        Appointment past = pastVisitWithoutReview(ME);

        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", past.getId(), "score", 9)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(ME, post("/api/mobile/reviews"),
                        Map.of("appointmentId", past.getId(), "score", 0)))
                .andExpect(status().isBadRequest());

        assertTrue(reviews.findByAppointment(past).isEmpty());
    }
}
