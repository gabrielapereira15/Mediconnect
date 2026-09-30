package com.vegs.mediconnect.backoffice;

import com.vegs.mediconnect.backoffice.auth.StaffSession;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who can reach what in the back office, checked through the running app.
 *
 * Hiding a link is not a permission, so these go to the addresses directly:
 * signed out, as the front desk, and as a clinician.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class BackOfficeAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffUserRepository staffUsers;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository scheduleTimeRepository;

    @Autowired
    private com.vegs.mediconnect.datasource.patient.PatientRepository patientRepository;

    private MockHttpSession signedInAs(String email) {
        var request = new MockHttpServletRequest();
        StaffSession.begin(request, staffUsers.findByEmailIgnoreCase(email).orElseThrow());
        return (MockHttpSession) request.getSession();
    }

    private MockHttpSession desk() {
        return signedInAs("desk@mediconnect.ca");
    }

    private MockHttpSession clinician() {
        return signedInAs("doctor@mediconnect.ca");
    }

    private Appointment todaysFirstUncheckedIn() {
        return appointmentRepository.findAllOnDay(LocalDate.now()).stream()
                .filter(appointment -> appointment.getCheckedInAt() == null)
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .findFirst()
                .orElse(null);
    }

    @Test
    @DisplayName("signed out, a back-office page sends you to sign in")
    void signedOutGoesToSignIn() throws Exception {
        mockMvc.perform(get("/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/staff/login?next=*"));
    }

    @Test
    @DisplayName("the scaffolded REST API and file upload are gone, not merely hidden")
    void oldOpenEndpointsAreClosed() throws Exception {
        // Guarded by default now, so even the addresses redirect to sign in
        // rather than answering with data.
        for (String path : new String[]{"/api/patients", "/api/appointments", "/api/doctors"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", startsWith("/staff/login")));
        }
        mockMvc.perform(get("/api/patients").session(desk()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/upload").session(desk()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("the mobile API is left to its own token check")
    void mobileApiIsNotRedirected() throws Exception {
        mockMvc.perform(get("/api/mobile/appointments/demo@mediconnect.ca"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the front desk sees the appointments list")
    void deskSeesAppointments() throws Exception {
        mockMvc.perform(get("/appointments").session(desk()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All bookings across the clinic")));
    }

    @Test
    @DisplayName("a clinician cannot check a patient in: that is the desk's")
    void clinicianCannotCheckIn() throws Exception {
        var appointment = todaysFirstUncheckedIn();
        org.junit.jupiter.api.Assumptions.assumeTrue(appointment != null, "no clinic on a weekend");

        mockMvc.perform(post("/appointments/" + appointment.getId() + "/check-in").session(clinician()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("the desk checks a patient in and lands back where it was")
    void deskChecksIn() throws Exception {
        var appointment = todaysFirstUncheckedIn();
        org.junit.jupiter.api.Assumptions.assumeTrue(appointment != null, "no clinic on a weekend");

        mockMvc.perform(post("/appointments/" + appointment.getId() + "/check-in")
                        .param("back", "/appointments?tab=today")
                        .session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/appointments?tab=today"));

        assertNotNull(appointmentRepository.findById(appointment.getId()).orElseThrow().getCheckedInAt());
    }

    @Test
    @DisplayName("a form's way back cannot be pointed at another site")
    void backCannotLeaveTheApp() throws Exception {
        var appointment = todaysFirstUncheckedIn();
        org.junit.jupiter.api.Assumptions.assumeTrue(appointment != null, "no clinic on a weekend");

        mockMvc.perform(post("/appointments/" + appointment.getId() + "/remind")
                        .param("back", "/\\evil.example")
                        .session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/appointments"));
    }

    @Test
    @DisplayName("the panel links to the chart for a clinician, and not for the desk")
    void panelLinksToChartForClinicianOnly() throws Exception {
        var appointment = appointmentRepository.findAll().getFirst();

        mockMvc.perform(get("/appointments/" + appointment.getId() + "/panel").session(clinician()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/patients/" + appointment.getPatient().getId())));
        mockMvc.perform(get("/appointments/" + appointment.getId() + "/panel").session(desk()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/patients/"))));
    }

    @Test
    @Transactional
    @DisplayName("the desk cancels with a reason, and nothing is deleted")
    void deskCancelsWithReason() throws Exception {
        var appointment = appointmentRepository.findAll().stream()
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getCanceled()))
                .filter(candidate -> candidate.getDateTime().isAfter(java.time.LocalDateTime.now().plusDays(1)))
                .findFirst().orElseThrow();
        long before = appointmentRepository.count();

        mockMvc.perform(post("/appointments/" + appointment.getId() + "/cancel")
                        .param("reason", "Called to say she is unwell")
                        .session(desk()))
                .andExpect(status().is3xxRedirection());

        var cancelled = appointmentRepository.findById(appointment.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(cancelled.getCanceled());
        org.junit.jupiter.api.Assertions.assertEquals("Called to say she is unwell", cancelled.getCancelReason());
        org.junit.jupiter.api.Assertions.assertEquals(before, appointmentRepository.count());
    }

    @Test
    @Transactional
    @DisplayName("the desk books a patient into a free time")
    void deskBooks() throws Exception {
        var slot = scheduleTimeRepository.findAll().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getAvailable()))
                .filter(candidate -> candidate.getDateTime().isAfter(java.time.LocalDateTime.now().plusDays(1)))
                .findFirst().orElseThrow();
        var patient = patientRepository.findAll().getFirst();

        mockMvc.perform(post("/appointments/new")
                        .param("patientId", patient.getId().toString())
                        .param("slotId", slot.getId().toString())
                        .param("note", "Walk-in follow-up")
                        .session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/appointments?selected=*"));

        org.junit.jupiter.api.Assertions.assertFalse(
                scheduleTimeRepository.findById(slot.getId()).orElseThrow().getAvailable());

        // Twice is refused: the time is gone.
        mockMvc.perform(post("/appointments/new")
                        .param("patientId", patient.getId().toString())
                        .param("slotId", slot.getId().toString())
                        .session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/appointments/new"));
    }

    @Test
    @DisplayName("the CSV export quotes every cell")
    void csvExport() throws Exception {
        mockMvc.perform(get("/appointments/export.csv").param("tab", "past").session(desk()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(startsWith("Date,Time,Patient,Date of birth,Booked for,Doctor,Status")))
                .andExpect(content().string(containsString("\"Gabriela Pereira\"")));
    }
}
