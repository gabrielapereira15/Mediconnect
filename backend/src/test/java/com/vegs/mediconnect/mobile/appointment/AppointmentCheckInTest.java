package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.doctor.DoctorApiService;
import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checking in for a visit.
 *
 * The front desk's queue is built from these timestamps, so the rules about
 * when one may be written matter more than most: a check-in for tomorrow
 * puts a patient in a queue they are not standing in, and a check-in on
 * somebody else's appointment is worse than useless.
 */
class AppointmentCheckInTest {

    private static final String EMAIL = "patient@example.com";
    private static final String SOMEONE_ELSE = "stranger@example.com";

    @Test
    @DisplayName("checking in on the day records the moment")
    void checkInOnTheDay() {
        var appointment = appointmentOn(LocalDate.now());
        var service = serviceFor(appointment);

        var before = OffsetDateTime.now();
        var response = service.checkIn(appointment.getId(), EMAIL);

        assertNotNull(appointment.getCheckedInAt(), "the arrival should be recorded");
        assertNotNull(response.getCheckedInAt(), "and reported back to the app");
        assertTrue(!appointment.getCheckedInAt().isBefore(before.minusMinutes(1)),
                "the time recorded should be now, not the appointment's own time");
    }

    @Test
    @DisplayName("checking in twice keeps the first arrival time")
    void checkInIsIdempotent() {
        var appointment = appointmentOn(LocalDate.now());
        var service = serviceFor(appointment);

        service.checkIn(appointment.getId(), EMAIL);
        OffsetDateTime first = appointment.getCheckedInAt();
        service.checkIn(appointment.getId(), EMAIL);

        // A patient whose screen reloads should not be moved to the back of
        // the queue for tapping the button again.
        assertEquals(first, appointment.getCheckedInAt());
    }

    @Test
    @DisplayName("checking in the day before is refused")
    void checkInBeforeTheDay() {
        var appointment = appointmentOn(LocalDate.now().plusDays(1));
        var service = serviceFor(appointment);

        assertThrows(CheckInNotOpenException.class,
                () -> service.checkIn(appointment.getId(), EMAIL));
    }

    @Test
    @DisplayName("a cancelled visit cannot be checked in to")
    void checkInOnCancelled() {
        var appointment = appointmentOn(LocalDate.now());
        appointment.setCanceled(true);
        var service = serviceFor(appointment);

        assertThrows(CheckInNotOpenException.class,
                () -> service.checkIn(appointment.getId(), EMAIL));
    }

    @Test
    @DisplayName("somebody else's appointment is reported as missing")
    void checkInOnAnotherPatientsVisit() {
        var appointment = appointmentOn(LocalDate.now());
        var service = serviceFor(appointment);

        // Missing rather than forbidden: "forbidden" would confirm the id
        // exists, which is the whole of what a guess needs.
        assertThrows(AppointmentNotFoundException.class,
                () -> service.checkIn(appointment.getId(), SOMEONE_ELSE));
    }

    // ---- fixtures ---------------------------------------------------------

    private Appointment appointmentOn(LocalDate date) {
        var doctor = new Doctor();
        doctor.setFirstName("Ana");
        doctor.setLastName("Costa");

        var schedule = new Schedule();
        schedule.setDate(date);
        schedule.setDoctor(doctor);

        var slot = new ScheduleTime();
        slot.setId(UUID.randomUUID());
        slot.setTime(LocalTime.of(10, 0));
        slot.setSchedule(schedule);
        slot.setAvailable(false);

        var patient = new Patient();
        patient.setEmail(EMAIL);

        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setCanceled(false);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduleTime(slot);
        return appointment;
    }

    private AppointmentApiService serviceFor(Appointment appointment) {
        var appointmentRepository = mock(AppointmentRepository.class);
        var patientRepository = mock(PatientRepository.class);
        var scheduleTimeRepository = mock(ScheduleTimeRepository.class);
        var reviewRepository = mock(ReviewRepository.class);
        var doctorApiService = mock(DoctorApiService.class);
        var waitlistService = mock(WaitlistService.class);

        when(appointmentRepository.findById(appointment.getId()))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorApiService.mapToDoctorSimpleResponse(any(Doctor.class)))
                .thenReturn(DoctorSimpleResponse.builder().build());

        return new AppointmentApiService(appointmentRepository, patientRepository,
                scheduleTimeRepository, doctorApiService, reviewRepository, waitlistService);
    }
}
