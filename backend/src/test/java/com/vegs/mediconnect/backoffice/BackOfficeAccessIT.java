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

    @Autowired
    private com.vegs.mediconnect.datasource.doctor.DoctorRepository doctorRepository;

    @Autowired
    private com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository waitlistRepository;

    @Autowired
    private com.vegs.mediconnect.datasource.notification.NotificationPatientRepository notificationPatientRepository;

    @Autowired
    private com.vegs.mediconnect.datasource.doctor.DoctorDayOffRepository dayOffRepository;

    @Autowired
    private com.vegs.mediconnect.backoffice.doctor.AvailabilityService availabilityService;

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
    @DisplayName("signing in lands on the page you were going to, and only inside the app")
    void signInFollowsNext() throws Exception {
        mockMvc.perform(post("/staff/login")
                        .param("email", "desk@mediconnect.ca")
                        .param("password", "demo")
                        .param("next", "/appointments?tab=today"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/appointments?tab=today"));
        mockMvc.perform(post("/staff/login")
                        .param("email", "desk@mediconnect.ca")
                        .param("password", "demo")
                        .param("next", "https://evil.example/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
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
    @DisplayName("the schedule shows the day, and a slot opens in the panel")
    void scheduleDay() throws Exception {
        mockMvc.perform(get("/schedule").session(clinician()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hx-get=\"/schedule/slots/")));
        mockMvc.perform(get("/schedule").param("view", "week").session(desk()))
                .andExpect(status().isOk());

        var slot = scheduleTimeRepository.findAll().getFirst();
        mockMvc.perform(get("/schedule/slots/" + slot.getId()).session(desk()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("the desk blocks and unblocks a free slot; a clinician cannot")
    void blockAndUnblock() throws Exception {
        var slot = scheduleTimeRepository.findAll().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getAvailable()))
                .filter(candidate -> candidate.getDateTime().isAfter(java.time.LocalDateTime.now().plusDays(1)))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/schedule/slots/" + slot.getId() + "/block").session(clinician()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/schedule/slots/" + slot.getId() + "/block").session(desk()))
                .andExpect(status().is3xxRedirection());
        var blocked = scheduleTimeRepository.findById(slot.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(blocked.getBlocked());
        org.junit.jupiter.api.Assertions.assertFalse(blocked.getAvailable());

        mockMvc.perform(post("/schedule/slots/" + slot.getId() + "/unblock").session(desk()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertTrue(
                scheduleTimeRepository.findById(slot.getId()).orElseThrow().getAvailable());
    }

    @Test
    @Transactional
    @DisplayName("saving a doctor's week rewrites the diary ahead and keeps every booking")
    void saveAvailability() throws Exception {
        var doctor = doctorRepository.findAll().stream()
                .filter(candidate -> candidate.getLastName().equals("Cameron"))
                .findFirst().orElseThrow();
        var from = java.time.LocalDate.now().plusDays(1);
        var to = java.time.LocalDate.now().plusWeeks(3);
        var bookedBefore = appointmentRepository.findAllBetween(from, to).stream()
                .filter(appointment -> appointment.getDoctor().getId().equals(doctor.getId()))
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .map(appointment -> appointment.getScheduleTime().getId())
                .toList();

        mockMvc.perform(post("/doctors/" + doctor.getId() + "/availability").session(clinician())
                        .param("slotMinutes", "30").param("weeks", "3"))
                .andExpect(status().isForbidden());

        // Mondays only, mornings only.
        mockMvc.perform(post("/doctors/" + doctor.getId() + "/availability").session(desk())
                        .param("slotMinutes", "30").param("weeks", "3")
                        .param("on_MONDAY", "on").param("start1_MONDAY", "09:00").param("end1_MONDAY", "11:00"))
                .andExpect(status().is3xxRedirection());

        var slots = scheduleTimeRepository.findAllBetween(from, to).stream()
                .filter(slot -> slot.getSchedule().getDoctor().getId().equals(doctor.getId()))
                .toList();
        for (var slot : slots) {
            boolean usual = slot.getSchedule().getDate().getDayOfWeek() == java.time.DayOfWeek.MONDAY
                    && slot.getTime().isBefore(java.time.LocalTime.of(11, 0));
            boolean booked = bookedBefore.contains(slot.getId());
            org.junit.jupiter.api.Assertions.assertTrue(usual || booked || Boolean.TRUE.equals(slot.getBlocked())
                            || !Boolean.TRUE.equals(slot.getAvailable()),
                    "a bookable slot outside the new hours: " + slot.getDateTime());
        }
        for (var slotId : bookedBefore) {
            org.junit.jupiter.api.Assertions.assertTrue(scheduleTimeRepository.existsById(slotId),
                    "a booked slot was removed");
        }
    }

    @Test
    @Transactional
    @DisplayName("a shorter horizon closes the free slots beyond it, and the diary rolls forward")
    void horizonIsEnforced() throws Exception {
        var doctor = doctorRepository.findAll().stream()
                .filter(candidate -> candidate.getLastName().equals("Hadley"))
                .findFirst().orElseThrow();
        var horizon = java.time.LocalDate.now().plusWeeks(1);

        var form = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/doctors/" + doctor.getId() + "/availability").session(desk())
                .param("slotMinutes", "30").param("weeks", "1");
        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"}) {
            form.param("on_" + day, "on").param("start1_" + day, "09:00").param("end1_" + day, "12:00");
        }
        mockMvc.perform(form).andExpect(status().is3xxRedirection());

        var beyond = scheduleTimeRepository.findAllBetween(horizon.plusDays(1), horizon.plusWeeks(4)).stream()
                .filter(slot -> slot.getSchedule().getDoctor().getId().equals(doctor.getId()))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .toList();
        org.junit.jupiter.api.Assertions.assertTrue(beyond.isEmpty(), "nothing bookable past the horizon");

        // A day inside the horizon that has lost its slots gets them back
        // on the next roll-forward.
        var day = java.time.LocalDate.now().plusDays(1);
        while (day.getDayOfWeek().getValue() > 5) {
            day = day.plusDays(1);
        }
        final var target = day;
        scheduleTimeRepository.findAllBetween(target, target).stream()
                .filter(slot -> slot.getSchedule().getDoctor().getId().equals(doctor.getId()))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .filter(slot -> appointmentRepository.findAllOnDay(target).stream()
                        .noneMatch(appointment -> appointment.getScheduleTime().getId().equals(slot.getId())))
                .forEach(scheduleTimeRepository::delete);
        scheduleTimeRepository.flush();

        org.junit.jupiter.api.Assertions.assertTrue(availabilityService.rollForward() > 0);
        org.junit.jupiter.api.Assertions.assertTrue(scheduleTimeRepository.findAllBetween(target, target).stream()
                .anyMatch(slot -> slot.getSchedule().getDoctor().getId().equals(doctor.getId())
                        && Boolean.TRUE.equals(slot.getAvailable())));
    }

    @Test
    @Transactional
    @DisplayName("a day off blocks the free slots, and taking it back reopens them")
    void dayOff() throws Exception {
        var doctor = doctorRepository.findAll().stream()
                .filter(candidate -> candidate.getLastName().equals("Taub"))
                .findFirst().orElseThrow();
        var date = java.time.LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }
        final var day = date;

        mockMvc.perform(post("/doctors/" + doctor.getId() + "/days-off").session(desk())
                        .param("date", day.toString()).param("reason", "Conference"))
                .andExpect(status().is3xxRedirection());
        java.util.function.Supplier<java.util.List<com.vegs.mediconnect.datasource.schedule.ScheduleTime>> slotsThatDay =
                () -> scheduleTimeRepository.findAllBetween(day, day).stream()
                        .filter(slot -> slot.getSchedule().getDoctor().getId().equals(doctor.getId()))
                        .toList();
        org.junit.jupiter.api.Assertions.assertTrue(slotsThatDay.get().stream()
                .noneMatch(slot -> Boolean.TRUE.equals(slot.getAvailable())), "nothing bookable on a day off");

        var off = dayOffRepository.findAllByDoctorOrderByDateAsc(doctor).getFirst();
        mockMvc.perform(post("/doctors/" + doctor.getId() + "/days-off/" + off.getId() + "/remove").session(desk()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertTrue(slotsThatDay.get().stream()
                .anyMatch(slot -> Boolean.TRUE.equals(slot.getAvailable())));
    }

    @Test
    @DisplayName("a photo is checked by what the file is, not what it claims")
    void photoMustBeAnImage() throws Exception {
        var doctor = doctorRepository.findAll().getFirst();
        var fake = new org.springframework.mock.web.MockMultipartFile("photo", "x.jpg", "image/jpeg",
                "<svg onload=alert(1)>".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/doctors/" + doctor.getId() + "/photo").file(fake).session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attribute("MSG_ERROR", "Use a JPEG, PNG or WebP photo."));
    }

    @Test
    @DisplayName("the doctors page and each panel tab render")
    void doctorsPage() throws Exception {
        var doctor = doctorRepository.findAll().getFirst();
        mockMvc.perform(get("/doctors").session(clinician()))
                .andExpect(status().isOk());
        for (String tab : new String[]{"profile", "availability", "reviews"}) {
            mockMvc.perform(get("/doctors/" + doctor.getId() + "/panel").param("tab", tab).session(desk()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("a patient's chart is a clinician's: the desk is refused on the server")
    void chartIsForClinicians() throws Exception {
        var patient = patientRepository.findAll().getFirst();

        mockMvc.perform(get("/patients").session(desk())).andExpect(status().isForbidden());
        mockMvc.perform(get("/patients/" + patient.getId()).session(desk())).andExpect(status().isForbidden());
        mockMvc.perform(get("/patients/" + patient.getId() + "/summary.json").session(desk()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/patients").session(clinician()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gabriela Pereira")));
        for (String tab : new String[]{"overview", "appointments", "forms", "record"}) {
            mockMvc.perform(get("/patients/" + patient.getId()).param("tab", tab).session(clinician()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Penicillin")));
        }
    }

    @Test
    @DisplayName("the chart exports the patient's PS-CA document")
    void chartExportsSummary() throws Exception {
        var patient = patientRepository.findAll().getFirst();

        mockMvc.perform(get("/patients/" + patient.getId() + "/summary.json").session(clinician()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("\"resourceType\": \"Bundle\"")));
    }

    @Test
    @DisplayName("the waitlist shows the held slot and every tab renders")
    void waitlistPage() throws Exception {
        mockMvc.perform(get("/waitlist").session(desk()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Held for Gabriela Pereira")));
        for (String tab : new String[]{"offered", "booked", "withdrawn"}) {
            mockMvc.perform(get("/waitlist").param("tab", tab).session(clinician()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/waitlist/new").session(clinician())).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("the desk adds someone, offers them a slot, then ends the offer")
    void deskOffersASlot() throws Exception {
        var visit = appointmentRepository.findAll().stream()
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getDoctor().getLastName().equals("Foreman"))
                .filter(appointment -> appointment.getDateTime().isAfter(java.time.LocalDateTime.now().plusDays(3)))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/waitlist/new").param("appointmentId", visit.getId().toString()).session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/waitlist"));
        var entry = waitlistRepository.findAllByPatientOrderByDateCreatedDesc(visit.getPatient()).stream()
                .filter(candidate -> candidate.getDoctor().getId().equals(visit.getDoctor().getId()))
                .findFirst().orElseThrow();

        var slot = scheduleTimeRepository.findAllBetween(java.time.LocalDate.now().plusDays(1),
                        visit.getDateTime().toLocalDate().minusDays(1)).stream()
                .filter(candidate -> candidate.getSchedule().getDoctor().getId().equals(visit.getDoctor().getId()))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getAvailable()))
                .filter(candidate -> com.vegs.mediconnect.mobile.waitlist.WaitlistService
                        .holdEnds(candidate, java.time.OffsetDateTime.now()) != null)
                .findFirst().orElseThrow();

        mockMvc.perform(post("/waitlist/" + entry.getId() + "/offer").param("slotId", slot.getId().toString())
                        .session(desk()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/waitlist?tab=offered"));
        org.junit.jupiter.api.Assertions.assertEquals(
                com.vegs.mediconnect.datasource.waitlist.WaitlistStatus.OFFERED, entry.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(slot.getAvailable(), "held, so off sale");

        mockMvc.perform(post("/waitlist/" + entry.getId() + "/end-offer").session(desk()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(
                com.vegs.mediconnect.datasource.waitlist.WaitlistStatus.WAITING, entry.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(slot.getAvailable(), "nobody else wanted it: back on sale");
    }

    @Test
    @DisplayName("messages list what was sent with read counts; a clinician can read but not send")
    void messagesPage() throws Exception {
        mockMvc.perform(get("/messages").session(clinician()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Flu shots available")));
        mockMvc.perform(post("/messages").param("title", "Hi").param("message", "Hello").session(clinician()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("the desk sends to one doctor's patients; links are refused; drafts reach nobody")
    void deskSendsAMessage() throws Exception {
        var chase = doctorRepository.findAll().stream()
                .filter(doctor -> doctor.getLastName().equals("Chase")).findFirst().orElseThrow();
        long before = notificationPatientRepository.count();

        mockMvc.perform(post("/messages").session(desk())
                        .param("audience", "doctor").param("doctorId", chase.getId().toString())
                        .param("title", "Clinic closed Friday afternoon")
                        .param("message", "Dr. Chase is away from 1pm. Your visit is not affected unless we call you."))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/messages"));
        org.junit.jupiter.api.Assertions.assertTrue(notificationPatientRepository.count() > before);

        long afterSend = notificationPatientRepository.count();
        mockMvc.perform(post("/messages").session(desk())
                        .param("audience", "all").param("title", "Update")
                        .param("message", "See https://example.com for details"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attributeExists("MSG_ERROR"));
        mockMvc.perform(post("/messages").session(desk()).param("action", "draft")
                        .param("title", "Holiday hours").param("message", "To follow."))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(afterSend, notificationPatientRepository.count(),
                "neither a refused message nor a draft reaches anyone");
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
