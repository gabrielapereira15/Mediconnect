package com.vegs.mediconnect.demo;

import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorHoursRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.staff.StaffUser;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The demo back-office logins, desk@ and doctor@mediconnect.ca with the
 * password "demo".
 *
 * That password is in the README and the log, so these two accounts must
 * only ever appear where the configuration asks for them: the in-memory
 * database a fresh clone starts with, never a Postgres somebody relies on.
 */
class DemoDataSeederStaffTest {

    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final StaffUserRepository staffUsers = mock(StaffUserRepository.class);
    private final DemoDataProperties properties = new DemoDataProperties();

    @BeforeEach
    void databaseAlreadyHasDoctors() {
        // So the seeder stops after the staff step and these tests are about
        // that step alone.
        when(doctors.count()).thenReturn(1L);
    }

    private DemoDataSeeder seeder() {
        return new DemoDataSeeder(
                doctors,
                mock(PatientRepository.class),
                mock(ScheduleRepository.class),
                mock(ScheduleTimeRepository.class),
                mock(AppointmentRepository.class),
                mock(NotificationRepository.class),
                mock(NotificationPatientRepository.class),
                mock(ReviewRepository.class),
                mock(HealthEntryRepository.class),
                staffUsers,
                mock(DoctorHoursRepository.class),
                mock(PreVisitFormRepository.class),
                mock(WaitlistEntryRepository.class),
                mock(WaitlistService.class),
                new BCryptPasswordEncoder(4),
                properties);
    }

    @Test
    @DisplayName("with the flag off, an empty staff table stays empty")
    void noDemoStaffWhenTheFlagIsOff() {
        properties.setStaffAccounts(false);

        seeder().run(new DefaultApplicationArguments());

        verify(staffUsers, never()).saveAll(any());
    }

    @Test
    @DisplayName("the flag is off unless the configuration turns it on")
    void offByDefault() {
        seeder().run(new DefaultApplicationArguments());

        verify(staffUsers, never()).saveAll(any());
    }

    @Test
    @DisplayName("with the flag on, an empty staff table gets one login per role")
    @SuppressWarnings("unchecked")
    void demoStaffWhenTheFlagIsOn() {
        properties.setStaffAccounts(true);

        seeder().run(new DefaultApplicationArguments());

        ArgumentCaptor<Iterable<StaffUser>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(staffUsers).saveAll(saved.capture());
        List<String> emails = new ArrayList<>();
        saved.getValue().forEach(user -> emails.add(user.getEmail()));
        assertEquals(List.of("desk@mediconnect.ca", "doctor@mediconnect.ca"), emails);
    }

    @Test
    @DisplayName("with the flag on, a staff table that has anyone in it is left alone")
    void noDemoStaffNextToRealOnes() {
        properties.setStaffAccounts(true);
        when(staffUsers.count()).thenReturn(1L);

        seeder().run(new DefaultApplicationArguments());

        verify(staffUsers, never()).saveAll(any());
    }
}
