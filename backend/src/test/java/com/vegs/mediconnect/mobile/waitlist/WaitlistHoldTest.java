package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.appointment.AppointmentApiService;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A freed slot, held for one waiting patient at a time (boards P08, P13).
 *
 * The rules that decide who gets an earlier appointment are the ones a
 * clinic has to be able to defend to the person who did not, so they are
 * written down here: longest-waiting first, never the patient who gave the
 * slot up, and a hold that is declined or runs out moves on by itself. A
 * patient who joined from a full day, holding nothing, is offered that day.
 */
class WaitlistHoldTest {

    private final Doctor doctor = new Doctor();
    private final List<WaitlistEntry> entries = new ArrayList<>();

    private WaitlistEntryRepository waitlistRepository;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private NotificationRepository notificationRepository;
    private AppointmentRepository appointmentRepository;
    private WaitlistService service;

    @BeforeEach
    void setUp() {
        doctor.setId(UUID.randomUUID());
        doctor.setFirstName("Robert");
        doctor.setLastName("Chase");

        waitlistRepository = mock(WaitlistEntryRepository.class);
        patientRepository = mock(PatientRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);

        // The repository answers from the entries as they are at the time
        // of the call, which is what the service's decisions depend on.
        when(waitlistRepository.findAllByDoctorAndStatusOrderByDateCreatedAsc(any(), any()))
                .thenAnswer(invocation -> entries.stream()
                        .filter(entry -> entry.getStatus() == invocation.getArgument(1))
                        .sorted(java.util.Comparator.comparing(WaitlistEntry::getDateCreated))
                        .toList());
        when(waitlistRepository.findAllByStatusAndOfferExpiresAtBefore(any(), any()))
                .thenAnswer(invocation -> entries.stream()
                        .filter(entry -> entry.getStatus() == invocation.getArgument(0))
                        .filter(entry -> entry.getOfferExpiresAt() != null
                                && entry.getOfferExpiresAt().isBefore(invocation.getArgument(1)))
                        .toList());
        when(waitlistRepository.findById(any())).thenAnswer(invocation -> entries.stream()
                .filter(entry -> entry.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(waitlistRepository.save(any(WaitlistEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        service = new WaitlistService(waitlistRepository, patientRepository,
                doctorRepository, mock(ScheduleTimeRepository.class),
                notificationRepository, mock(NotificationPatientRepository.class),
                appointmentRepository);
    }

    @Test
    @DisplayName("a freed slot is held for the longest-waiting patient, and only them")
    void heldForTheFirstInLine() {
        var first = waiting("first@example.com", 3);
        var second = waiting("second@example.com", 1);
        var slot = slotInDays(2);

        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));

        assertEquals(WaitlistStatus.OFFERED, first.getStatus());
        assertEquals(slot, first.getOfferedSlot());
        assertNotNull(first.getOfferExpiresAt());
        assertEquals(WaitlistStatus.WAITING, second.getStatus(), "one hold at a time");
        assertFalse(slot.getAvailable(), "a held slot is off sale for everyone else");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("the patient who gave the slot up is not offered it back")
    void neverTheOneWhoCancelled() {
        var canceller = waiting("canceller@example.com", 5);
        var next = waiting("next@example.com", 1);
        var slot = slotInDays(2);

        service.offerFreedSlot(cancelledBy(canceller.getPatient(), slot));

        assertEquals(WaitlistStatus.WAITING, canceller.getStatus());
        assertEquals(WaitlistStatus.OFFERED, next.getStatus());
    }

    @Test
    @DisplayName("turning an offer down passes the slot to the next person")
    void declinePassesItOn() {
        var first = waiting("first@example.com", 3);
        var second = waiting("second@example.com", 1);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        when(patientRepository.findByEmail("first@example.com"))
                .thenReturn(Optional.of(first.getPatient()));

        service.decline("first@example.com", first.getId());

        assertEquals(WaitlistStatus.WAITING, first.getStatus(), "still on the list for the next one");
        assertEquals(slot.getId(), first.getPassedSlotId());
        assertNull(first.getOfferedSlot());
        assertEquals(WaitlistStatus.OFFERED, second.getStatus());
        assertEquals(slot, second.getOfferedSlot());
    }

    @Test
    @DisplayName("when nobody else wants it, a declined slot goes back on sale")
    void lastDeclineFreesTheSlot() {
        var only = waiting("only@example.com", 3);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        when(patientRepository.findByEmail("only@example.com"))
                .thenReturn(Optional.of(only.getPatient()));

        service.decline("only@example.com", only.getId());

        assertTrue(slot.getAvailable());
        assertEquals(WaitlistStatus.WAITING, only.getStatus());
    }

    @Test
    @DisplayName("a hold that runs out moves on by itself")
    void lapsedHoldsMoveOn() {
        var first = waiting("first@example.com", 3);
        var second = waiting("second@example.com", 1);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        first.setOfferExpiresAt(OffsetDateTime.now().minusMinutes(1));

        service.expireOffers();

        assertEquals(WaitlistStatus.WAITING, first.getStatus());
        assertEquals(WaitlistStatus.OFFERED, second.getStatus());
    }

    @Test
    @DisplayName("a hold ends in time for the booking rules to still allow it")
    void holdIsCappedByTheLeadTime() {
        var now = OffsetDateTime.now();

        var soonish = slotAt(LocalDateTime.now().plusHours(7));
        var ends = WaitlistService.holdEnds(soonish, now);
        assertNotNull(ends);
        assertTrue(ends.isBefore(now.plusHours(1).plusMinutes(1)),
                "six hours' notice is still needed to book it");

        var tooSoon = slotAt(LocalDateTime.now().plusHours(6).plusMinutes(5));
        assertNull(WaitlistService.holdEnds(tooSoon, now), "not worth a five-minute hold");
    }

    @Test
    @DisplayName("taking an offer books the held slot and gives up the old appointment")
    void acceptMovesThePatient() {
        var entry = waiting("first@example.com", 3);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        when(patientRepository.findByEmail("first@example.com"))
                .thenReturn(Optional.of(entry.getPatient()));

        var current = cancelledBy(entry.getPatient(), slotOn(entry.getCurrentAppointmentDate()));
        current.setCanceled(false);
        when(appointmentRepository.findAll()).thenReturn(List.of(current));
        var appointments = mock(AppointmentApiService.class);
        var offers = new WaitlistOfferService(service, waitlistRepository,
                appointmentRepository, appointments);

        offers.accept("first@example.com", entry.getId());

        assertEquals(WaitlistStatus.BOOKED, entry.getStatus());
        verify(appointments).bookHeldSlot(eq(slot), eq(entry.getPatient()), eq(current));
        verify(appointments).removeAppointment(current.getId());
    }

    @Test
    @DisplayName("an offer that has ended cannot be taken")
    void acceptAfterExpiry() {
        var entry = waiting("first@example.com", 3);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        entry.setOfferExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(patientRepository.findByEmail("first@example.com"))
                .thenReturn(Optional.of(entry.getPatient()));
        var appointments = mock(AppointmentApiService.class);
        var offers = new WaitlistOfferService(service, waitlistRepository,
                mock(AppointmentRepository.class), appointments);

        assertThrows(OfferEndedException.class,
                () -> offers.accept("first@example.com", entry.getId()));
        verify(appointments, never()).bookHeldSlot(any(), any(), any());
    }

    @Test
    @DisplayName("taking an offer while holding no visit books it and gives nothing up")
    void acceptWithNothingHeld() {
        var entry = waiting("asker@example.com", 3);
        entry.setHoldsVisit(false);
        var slot = slotInDays(2);
        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));
        when(patientRepository.findByEmail("asker@example.com"))
                .thenReturn(Optional.of(entry.getPatient()));
        var appointments = mock(AppointmentApiService.class);
        var offers = new WaitlistOfferService(service, waitlistRepository,
                mock(AppointmentRepository.class), appointments);

        offers.accept("asker@example.com", entry.getId());

        assertEquals(WaitlistStatus.BOOKED, entry.getStatus());
        verify(appointments).bookHeldSlot(eq(slot), eq(entry.getPatient()), isNull());
        verify(appointments, never()).removeAppointment(any(UUID.class));
    }

    // ---- joining from a full day ----------------------------------------------

    @Test
    @DisplayName("joining with the date of a visit they hold records that they hold it")
    void joinFromAHeldVisit() {
        var patient = signedIn("holder@example.com");
        var day = LocalDate.now().plusDays(10);
        var visit = cancelledBy(patient, slotOn(day));
        visit.setCanceled(false);
        when(appointmentRepository.findAllByPatient(patient)).thenReturn(List.of(visit));

        var response = service.join("holder@example.com", joining(day));

        assertTrue(response.isHoldsVisit());
    }

    @Test
    @DisplayName("joining from a full day records that they hold no visit on it")
    void joinFromAFullDay() {
        var patient = signedIn("asker@example.com");
        var day = LocalDate.now().plusDays(10);
        // A visit on that day they have since cancelled, and one with a
        // different doctor, are neither of them a visit being improved on.
        var cancelled = cancelledBy(patient, slotOn(day));
        var otherDoctor = new Doctor();
        otherDoctor.setId(UUID.randomUUID());
        var elsewhere = cancelledBy(patient, slotOn(day));
        elsewhere.setCanceled(false);
        elsewhere.setDoctor(otherDoctor);
        when(appointmentRepository.findAllByPatient(patient))
                .thenReturn(List.of(cancelled, elsewhere));

        var response = service.join("asker@example.com", joining(day));

        assertFalse(response.isHoldsVisit());
    }

    @Test
    @DisplayName("a slot freed on a full day goes to the patient who asked for that day")
    void freedOnTheAskedForDay() {
        var holder = waiting("holder@example.com", 5);
        var asker = waiting("asker@example.com", 1);
        asker.setHoldsVisit(false);
        var slot = slotOn(asker.getCurrentAppointmentDate());

        service.offerFreedSlot(cancelledBy(patient("gone@example.com"), slot));

        assertEquals(WaitlistStatus.WAITING, holder.getStatus(),
                "the same day is no improvement on a visit they already hold there");
        assertEquals(WaitlistStatus.OFFERED, asker.getStatus(),
                "a slot on the day they asked for is the one they joined for");
        assertEquals(slot, asker.getOfferedSlot());
    }

    @Test
    @DisplayName("booking the day they asked for takes them off the list")
    void bookingTheAskedForDay() {
        var holder = waiting("holder@example.com", 5);
        var asker = waiting("asker@example.com", 1);
        asker.setHoldsVisit(false);
        var day = asker.getCurrentAppointmentDate();
        when(waitlistRepository.findAllByPatientOrderByDateCreatedDesc(holder.getPatient()))
                .thenReturn(List.of(holder));
        when(waitlistRepository.findAllByPatientOrderByDateCreatedDesc(asker.getPatient()))
                .thenReturn(List.of(asker));

        service.onAppointmentBooked(holder.getPatient(), doctor, day);
        service.onAppointmentBooked(asker.getPatient(), doctor, day);

        assertEquals(WaitlistStatus.WAITING, holder.getStatus(),
                "a second booking on the day they hold is not the earlier one they wanted");
        assertEquals(WaitlistStatus.BOOKED, asker.getStatus(),
                "otherwise they would be offered the day they already have");
    }

    // ---- fixtures -------------------------------------------------------------

    private WaitlistEntry waiting(String email, int daysOnTheList) {
        var entry = new WaitlistEntry();
        entry.setId(UUID.randomUUID());
        entry.setPatient(patient(email));
        entry.setDoctor(doctor);
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setCurrentAppointmentDate(LocalDate.now().plusDays(10));
        entry.setHoldsVisit(true);
        entry.setDateCreated(OffsetDateTime.now().minusDays(daysOnTheList));
        entries.add(entry);
        return entry;
    }

    /** A patient the service can find by email, as the signed-in caller. */
    private Patient signedIn(String email) {
        var patient = patient(email);
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));
        return patient;
    }

    private WaitlistRequest joining(LocalDate day) {
        var request = new WaitlistRequest();
        request.setDoctorId(doctor.getId());
        request.setCurrentAppointmentDate(day);
        return request;
    }

    private Patient patient(String email) {
        var patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setEmail(email);
        return patient;
    }

    private ScheduleTime slotInDays(int days) {
        return slotAt(LocalDate.now().plusDays(days).atTime(10, 0));
    }

    private ScheduleTime slotOn(LocalDate date) {
        return slotAt(date.atTime(9, 0));
    }

    private ScheduleTime slotAt(LocalDateTime at) {
        var schedule = new Schedule();
        schedule.setDate(at.toLocalDate());
        schedule.setDoctor(doctor);
        var slot = new ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(at.toLocalTime());
        slot.setSchedule(schedule);
        slot.setAvailable(true);
        return slot;
    }

    private Appointment cancelledBy(Patient patient, ScheduleTime slot) {
        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduleTime(slot);
        appointment.setCanceled(true);
        return appointment;
    }

    @SuppressWarnings("unused")
    private static OffsetDateTime at(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
