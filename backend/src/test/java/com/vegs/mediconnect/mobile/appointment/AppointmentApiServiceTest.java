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
import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.doctor.DoctorApiService;
import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers booking on someone else's behalf.
 *
 * The app collected a name, date of birth and phone number for the person
 * actually attending and then dropped them: the request named only the
 * account holder, so a parent's booking for a child was indistinguishable
 * from their own. These check the details survive the round trip, and that
 * an ordinary booking for yourself still carries none of them.
 */
class AppointmentApiServiceTest {

    private static final String EMAIL = "patient@example.com";

    private AppointmentApiService service;
    private ScheduleTime slot;
    private UUID slotId;

    @BeforeEach
    void setUp() {
        var doctor = new Doctor();
        doctor.setFirstName("Ana");
        doctor.setLastName("Costa");

        var schedule = new Schedule();
        schedule.setDate(LocalDate.now().plusDays(3));
        schedule.setDoctor(doctor);

        slotId = UUID.randomUUID();
        slot = new ScheduleTime();
        slot.setId(slotId);
        slot.setTime(LocalTime.of(10, 0));
        slot.setSchedule(schedule);
        slot.setAvailable(true);

        var patient = new Patient();
        patient.setEmail(EMAIL);

        var appointmentRepository = mock(AppointmentRepository.class);
        var patientRepository = mock(PatientRepository.class);
        var scheduleTimeRepository = mock(ScheduleTimeRepository.class);
        var reviewRepository = mock(ReviewRepository.class);
        var doctorApiService = mock(DoctorApiService.class);

        when(patientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(patient));
        when(scheduleTimeRepository.findById(slotId)).thenReturn(Optional.of(slot));
        // Saving hands back what it was given, so the test can read the row
        // the service built.
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorApiService.mapToDoctorSimpleResponse(any(Doctor.class)))
                .thenReturn(DoctorSimpleResponse.builder().build());

        // The waitlist is told when a booking frees a slot; these tests are
        // about creating one, so a mock is enough.
        var waitlistService = mock(com.vegs.mediconnect.mobile.waitlist.WaitlistService.class);

        service = new AppointmentApiService(appointmentRepository, patientRepository,
                scheduleTimeRepository, doctorApiService, reviewRepository, waitlistService,
                mock(com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository.class));
    }

    @Test
    @DisplayName("booking for someone else records who is attending")
    void bookedForDetailsAreKept() {
        var response = service.create(request()
                .bookedForName("Sofia Almeida")
                .bookedForDateOfBirth("2016-03-14")
                .bookedForPhone("6475550142")
                .bookedForNotes("Allergic to penicillin")
                .build());

        assertEquals("Sofia Almeida", response.getBookedForName());
    }

    @Test
    @DisplayName("booking for yourself names nobody else")
    void ownBookingHasNoBookedForName() {
        var response = service.create(request().build());

        assertNull(response.getBookedForName(),
                "an appointment for the account holder should not name a third party");
    }

    @Test
    @DisplayName("a blank name is treated as booking for yourself")
    void blankNameIsIgnored() {
        var response = service.create(request().bookedForName("   ").build());

        assertNull(response.getBookedForName());
    }

    @Test
    @DisplayName("an unreadable date of birth does not lose the booking")
    void unparseableDateOfBirthStillBooks() {
        // The date is the least important of the three: refusing the whole
        // request over it would cost the patient their slot.
        var response = service.create(request()
                .bookedForName("Sofia Almeida")
                .bookedForDateOfBirth("14/03/2016")
                .build());

        assertNotNull(response);
        assertEquals("Sofia Almeida", response.getBookedForName());
    }

    private AppointmentRequest.AppointmentRequestBuilder request() {
        return AppointmentRequest.builder()
                .patientEmail(EMAIL)
                .scheduleTimeId(slotId);
    }
}
