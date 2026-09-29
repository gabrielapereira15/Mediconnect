package com.vegs.mediconnect.demo;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.review.Review;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates an empty database with a small, believable clinic so a fresh clone
 * has something to show.
 *
 * Runs only when {@code mediconnect.demo-data.enabled} is true (the default
 * outside the production profile) and only when there are no doctors yet, so
 * restarting against a real Postgres never duplicates rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mediconnect.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String CLINIC_CODE = "KIT001";

    /** Appointment slots offered each working day. */
    private static final List<LocalTime> SLOT_TIMES = List.of(
            LocalTime.of(9, 0), LocalTime.of(9, 30),
            LocalTime.of(10, 0), LocalTime.of(10, 30),
            LocalTime.of(11, 0), LocalTime.of(11, 30),
            LocalTime.of(13, 0), LocalTime.of(13, 30),
            LocalTime.of(14, 0), LocalTime.of(14, 30),
            LocalTime.of(15, 0), LocalTime.of(15, 30)
    );

    /** Schedules run from this many days in the past (for appointment history)… */
    private static final int SCHEDULE_DAYS_PAST = 21;

    /** …to this many days ahead (for booking). */
    private static final int SCHEDULE_DAYS_AHEAD = 21;

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (doctorRepository.count() > 0) {
            log.info("Demo data: database already has doctors, skipping seed.");
            return;
        }

        log.info("Demo data: seeding an empty database…");

        List<Doctor> doctors = seedDoctors();
        List<Patient> patients = seedPatients();
        List<ScheduleTime> slots = seedSchedules(doctors);
        List<Appointment> appointments = seedAppointments(doctors, patients, slots);
        seedReviews(doctors, appointments);
        seedNotifications(patients);

        log.info("Demo data: seeded {} doctors, {} patients, {} slots, {} appointments.",
                doctors.size(), patients.size(), slots.size(), appointments.size());
        log.info("Demo data: sign in from the app with {}", patients.getFirst().getEmail());
    }

    private List<Doctor> seedDoctors() {
        List<Doctor> doctors = List.of(
                doctor("Robert", "Chase", "Surgeon", "12",
                        "Consultant surgeon with a focus on minimally invasive procedures. "
                                + "Trained in Melbourne and practising in Ontario since 2015."),
                doctor("Lisa", "Cuddy", "Optometrist", "18",
                        "Optometrist specialising in paediatric vision and contact lens fitting."),
                doctor("Allison", "Cameron", "Cardiologist", "9",
                        "Cardiologist with an interest in preventive care and remote monitoring."),
                doctor("Eric", "Foreman", "Neurologist", "14",
                        "Neurologist treating headache disorders, epilepsy and movement disorders."),
                doctor("Remy", "Hadley", "Gynecologist", "7",
                        "Gynecologist providing routine screening, family planning and prenatal care."),
                doctor("James", "Wilson", "Pediatrician", "16",
                        "Paediatrician caring for newborns through to adolescents, including "
                                + "developmental assessments and immunisation."),
                doctor("Chris", "Taub", "Dermatologist", "11",
                        "Dermatologist covering skin cancer screening, acne and eczema."),
                doctor("Amber", "Volakis", "General Practitioner", "6",
                        "Family doctor offering annual check-ups, chronic disease management "
                                + "and same-day appointments.")
        );

        return doctorRepository.saveAll(doctors);
    }

    private Doctor doctor(String firstName, String lastName, String specialty,
                          String years, String about) {
        var doctor = new Doctor();
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setSpecialty(specialty);
        doctor.setExperienceInYears(years);
        doctor.setAbout(about);
        return doctor;
    }

    private List<Patient> seedPatients() {
        List<Patient> patients = List.of(
                patient("demo@mediconnect.ca", "Gabriela", "Pereira", "Female",
                        "1995-04-12", "226-899-0000", "15 Wellington Street, Kitchener, ON"),
                patient("john.doe@example.com", "John", "Doe", "Male",
                        "1988-11-03", "519-555-0142", "88 King Street West, Kitchener, ON")
        );

        return patientRepository.saveAll(patients);
    }

    private Patient patient(String email, String firstName, String lastName, String gender,
                            String birthdate, String phone, String address) {
        var patient = new Patient();
        patient.setEmail(email);
        patient.setClinicCode(CLINIC_CODE);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setGender(gender);
        patient.setBirthdate(birthdate);
        patient.setPhoneNumber(phone);
        patient.setAddress(address);
        return patient;
    }

    /** Gives every doctor a slot grid over the next few weeks, weekdays only. */
    private List<ScheduleTime> seedSchedules(List<Doctor> doctors) {
        var schedules = new ArrayList<Schedule>();
        var times = new ArrayList<ScheduleTime>();
        LocalDate today = LocalDate.now();

        for (Doctor doctor : doctors) {
            for (int day = -SCHEDULE_DAYS_PAST; day < SCHEDULE_DAYS_AHEAD; day++) {
                LocalDate date = today.plusDays(day);
                if (date.getDayOfWeek().getValue() > 5) {
                    continue; // clinic is closed at weekends
                }

                var schedule = new Schedule();
                schedule.setDoctor(doctor);
                schedule.setDate(date);
                schedule.setAvailable(true);
                schedules.add(schedule);
            }
        }

        scheduleRepository.saveAll(schedules);

        for (Schedule schedule : schedules) {
            for (LocalTime time : SLOT_TIMES) {
                var slot = new ScheduleTime();
                slot.setSchedule(schedule);
                slot.setTime(time);
                slot.setAvailable(true);
                times.add(slot);
            }
        }

        return scheduleTimeRepository.saveAll(times);
    }

    /**
     * A handful of appointments for the demo patient, one in each state so the
     * Upcoming / Completed / Cancelled tabs all have something in them.
     */
    private List<Appointment> seedAppointments(List<Doctor> doctors, List<Patient> patients,
                                               List<ScheduleTime> slots) {
        Patient demoPatient = patients.getFirst();
        var appointments = new ArrayList<Appointment>();

        // Two still to come, so the Upcoming tab has something.
        appointments.add(appointment(demoPatient, doctors.get(0),
                findSlot(slots, doctors.get(0), businessDaysFromToday(2), LocalTime.of(10, 0)),
                AppointmentStatus.UPCOMING));
        appointments.add(appointment(demoPatient, doctors.get(2),
                findSlot(slots, doctors.get(2), businessDaysFromToday(5), LocalTime.of(14, 0)),
                AppointmentStatus.UPCOMING));

        // One booked for a family member, so the Upcoming tab shows both an
        // appointment for yourself and one that names someone else.
        var forDaughter = appointment(demoPatient, doctors.get(3),
                findSlot(slots, doctors.get(3), businessDaysFromToday(4), LocalTime.of(9, 0)),
                AppointmentStatus.UPCOMING);
        forDaughter.setBookedForName("Sofia Almeida");
        forDaughter.setBookedForDateOfBirth(LocalDate.of(2016, 3, 14));
        forDaughter.setBookedForPhone("6475550142");
        appointments.add(forDaughter);

        // Three in the past, each of which can carry a review.
        appointments.add(appointment(demoPatient, doctors.get(1),
                findSlot(slots, doctors.get(1), businessDaysFromToday(-6), LocalTime.of(9, 30)),
                AppointmentStatus.COMPLETED));
        appointments.add(appointment(demoPatient, doctors.get(0),
                findSlot(slots, doctors.get(0), businessDaysFromToday(-13), LocalTime.of(11, 0)),
                AppointmentStatus.COMPLETED));
        appointments.add(appointment(demoPatient, doctors.get(2),
                findSlot(slots, doctors.get(2), businessDaysFromToday(-14), LocalTime.of(15, 0)),
                AppointmentStatus.COMPLETED));
        appointments.add(appointment(demoPatient, doctors.get(7),
                findSlot(slots, doctors.get(7), businessDaysFromToday(-9), LocalTime.of(13, 30)),
                AppointmentStatus.COMPLETED));

        // And one the patient called off.
        appointments.add(appointment(demoPatient, doctors.get(5),
                findSlot(slots, doctors.get(5), businessDaysFromToday(3), LocalTime.of(11, 0)),
                AppointmentStatus.CANCELED));

        return appointmentRepository.saveAll(appointments);
    }

    private Appointment appointment(Patient patient, Doctor doctor, ScheduleTime slot,
                                    AppointmentStatus status) {
        var appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduleTime(slot);
        appointment.setStatus(status.getStatus());
        appointment.setCanceled(status == AppointmentStatus.CANCELED);

        if (slot != null) {
            // A booked slot is no longer offered to anyone else.
            slot.setAvailable(status == AppointmentStatus.CANCELED);
            scheduleTimeRepository.save(slot);
        }

        return appointment;
    }

    /** The nth clinic day from today, skipping weekends. Negative counts back. */
    private static LocalDate businessDaysFromToday(int days) {
        LocalDate date = LocalDate.now();
        int step = days < 0 ? -1 : 1;
        for (int moved = 0; moved < Math.abs(days); ) {
            date = date.plusDays(step);
            if (date.getDayOfWeek().getValue() <= 5) {
                moved++;
            }
        }
        // The starting day may itself be a weekend.
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(step);
        }
        return date;
    }

    /**
     * Finds a specific slot for a doctor.
     *
     * The fallback stays on the same side of today as the requested date —
     * otherwise a missed future slot silently becomes a past one, and an
     * appointment meant to be upcoming shows up as history.
     */
    private ScheduleTime findSlot(List<ScheduleTime> slots, Doctor doctor,
                                  LocalDate date, LocalTime time) {
        boolean wantFuture = !date.isBefore(LocalDate.now());

        return slots.stream()
                .filter(slot -> sameDoctor(slot, doctor))
                .filter(slot -> slot.getSchedule().getDate().equals(date) && slot.getTime().equals(time))
                .findFirst()
                .orElseGet(() -> slots.stream()
                        .filter(slot -> sameDoctor(slot, doctor))
                        .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                        .filter(slot -> wantFuture
                                ? !slot.getSchedule().getDate().isBefore(LocalDate.now())
                                : slot.getSchedule().getDate().isBefore(LocalDate.now()))
                        .findFirst()
                        .orElse(null));
    }

    private static boolean sameDoctor(ScheduleTime slot, Doctor doctor) {
        return slot.getSchedule().getDoctor().getId().equals(doctor.getId());
    }

    /**
     * Reviews a couple of past visits, deliberately not all of them.
     *
     * review.appointment_id is NOT NULL and UNIQUE, so a review needs its own
     * visit — and the app hides "Add Review" on a visit that already has one.
     * Leaving some unreviewed is what makes that flow reachable in the demo.
     */
    private void seedReviews(List<Doctor> doctors, List<Appointment> appointments) {
        List<String> comments = List.of(
                "Very thorough and took the time to explain everything clearly.",
                "Excellent care and the follow-up was well organised."
        );
        List<Float> scores = List.of(5f, 4.5f);

        List<Appointment> completed = appointments.stream()
                .filter(a -> AppointmentStatus.COMPLETED.getStatus().equals(a.getStatus()))
                .toList();

        var reviews = new ArrayList<Review>();
        for (int i = 0; i < completed.size() && i < comments.size(); i++) {
            Appointment visit = completed.get(i);
            reviews.add(review(visit.getDoctor(), visit, scores.get(i), comments.get(i)));
        }

        reviewRepository.saveAll(reviews);
    }

    private Review review(Doctor doctor, Appointment appointment, Float score, String description) {
        var review = new Review();
        review.setDoctor(doctor);
        review.setAppointment(appointment);
        review.setScore(score);
        review.setDescription(description);
        return review;
    }

    private void seedNotifications(List<Patient> patients) {
        var notifications = List.of(
                notification("Appointment reminder",
                        "You have an appointment with Dr. Chase in two days. "
                                + "Please complete your pre-appointment form beforehand."),
                notification("Flu shots available",
                        "Walk-in flu vaccinations are available at the clinic every weekday "
                                + "between 9am and 4pm, no appointment needed."),
                notification("Clinic hours over the holidays",
                        "The clinic will close at 1pm on December 24th and reopen on December 27th.")
        );

        List<Notification> saved = notificationRepository.saveAll(notifications);

        var links = new ArrayList<NotificationPatient>();
        for (Notification notification : saved) {
            for (Patient patient : patients) {
                var link = new NotificationPatient();
                link.setNotificationId(notification);
                link.setPatientId(patient);
                link.setAcknowledged(false);
                links.add(link);
            }
        }

        notificationPatientRepository.saveAll(links);
    }

    private Notification notification(String title, String message) {
        var notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSendAllPatients(true);
        notification.setIsDeleted(false);
        return notification;
    }
}
