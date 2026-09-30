package com.vegs.mediconnect.demo;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.datasource.staff.StaffUser;
import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.time.LocalDateTime;
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
    private final com.vegs.mediconnect.datasource.health.HealthEntryRepository healthEntryRepository;
    private final StaffUserRepository staffUserRepository;
    private final com.vegs.mediconnect.datasource.doctor.DoctorHoursRepository doctorHoursRepository;
    private final com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository preVisitFormRepository;
    private final com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository waitlistEntryRepository;
    private final com.vegs.mediconnect.mobile.waitlist.WaitlistService waitlistService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Staff accounts are seeded first and on their own guard: they were
        // added after the rest, and a database that already had doctors in
        // it would otherwise never get them.
        seedStaff();

        if (doctorRepository.count() > 0) {
            log.info("Demo data: database already has doctors, skipping seed.");
            return;
        }

        log.info("Demo data: seeding an empty database…");

        List<Doctor> doctors = seedDoctors();
        List<Patient> patients = seedPatients();
        List<ScheduleTime> slots = seedSchedules(doctors);
        seedWeeklyHours(doctors);
        List<Appointment> appointments = seedAppointments(doctors, patients, slots);
        seedForms(appointments);
        seedReviews(doctors, appointments);
        seedNotifications(patients, appointments);
        seedWaitlistOffer(patients.getFirst(), doctors.get(2), appointments);
        seedHealthRecord(patients.getFirst());

        log.info("Demo data: seeded {} doctors, {} patients, {} slots, {} appointments.",
                doctors.size(), patients.size(), slots.size(), appointments.size());
        log.info("Demo data: sign in from the app with {}", patients.getFirst().getEmail());
        log.info("Demo data: sign in to the back office with desk@mediconnect.ca "
                + "or doctor@mediconnect.ca, password \"demo\"");
    }

    /**
     * Two accounts, one per role, so the difference between them can be
     * seen rather than described.
     *
     * The password is hashed even here. A seeded plaintext password is how
     * one ends up in production.
     */
    private void seedStaff() {
        if (staffUserRepository.count() > 0) {
            return;
        }
        staffUserRepository.saveAll(List.of(
                staff("desk@mediconnect.ca", "Ana Ferreira", StaffRole.FRONT_DESK),
                staff("doctor@mediconnect.ca", "Robert Chase", StaffRole.CLINICIAN)));
    }

    private StaffUser staff(String email, String name, StaffRole role) {
        var user = new StaffUser();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setClinicCode("KIT001");
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode("demo"));
        return user;
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
        // Exercises the CA Baseline jurisdictional health number identifier.
        patient.setHealthCardNumber("1234567890");
        patient.setHealthCardProvince("ON");
        patient.setAddress(address);
        return patient;
    }

    /**
     * The usual week behind the seeded slots — weekdays, 9:00–12:00 and
     * 13:00–16:00 in half hours, three weeks ahead — so the back office's
     * Availability tab shows it rather than working it out.
     */
    private void seedWeeklyHours(List<Doctor> doctors) {
        var rows = new ArrayList<com.vegs.mediconnect.datasource.doctor.DoctorHours>();
        for (Doctor doctor : doctors) {
            doctor.setSlotMinutes(30);
            doctor.setBookingWeeks(3);
            for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
                if (day.getValue() > 5) {
                    continue;
                }
                rows.add(hours(doctor, day, LocalTime.of(9, 0), LocalTime.of(12, 0)));
                rows.add(hours(doctor, day, LocalTime.of(13, 0), LocalTime.of(16, 0)));
            }
        }
        doctorRepository.saveAll(doctors);
        doctorHoursRepository.saveAll(rows);
    }

    private com.vegs.mediconnect.datasource.doctor.DoctorHours hours(Doctor doctor, java.time.DayOfWeek day,
                                                                       LocalTime start, LocalTime end) {
        var row = new com.vegs.mediconnect.datasource.doctor.DoctorHours();
        row.setDoctor(doctor);
        row.setDayOfWeek(day);
        row.setStartTime(start);
        row.setEndTime(end);
        return row;
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

        appointments.addAll(todaysClinic(doctors, patients));

        return appointmentRepository.saveAll(appointments);
    }

    /**
     * A day in progress, for the back office's Today board.
     *
     * Without this the clinic's main screen opened empty on a fresh
     * database: every seeded appointment was days out or long past, so the
     * one page the front desk lives on had nothing to show. These sit
     * across the working day — some already seen, one in the waiting room,
     * the rest still to come, and a couple with the form still outstanding.
     */
    private List<Appointment> todaysClinic(List<Doctor> doctors, List<Patient> patients) {
        LocalDate today = LocalDate.now();
        var todays = new ArrayList<Appointment>();

        // Nothing to show on a day the clinic does not open.
        if (today.getDayOfWeek().getValue() > 5) {
            return todays;
        }

        Patient first = patients.getFirst();
        Patient second = patients.size() > 1 ? patients.get(1) : first;

        record Booking(int doctor, Patient patient, LocalTime time, boolean formIn,
                       boolean arrived) {
        }

        var bookings = List.of(
                new Booking(1, second, LocalTime.of(9, 0), true, true),
                new Booking(0, first, LocalTime.of(9, 30), true, true),
                new Booking(3, second, LocalTime.of(10, 0), false, false),
                new Booking(0, first, LocalTime.of(11, 0), true, false),
                new Booking(2, second, LocalTime.of(13, 30), false, false),
                new Booking(1, first, LocalTime.of(14, 30), true, false),
                new Booking(4, second, LocalTime.of(15, 30), false, false));

        for (Booking booking : bookings) {
            Doctor doctor = doctors.get(booking.doctor());
            ScheduleTime slot = findSlotOn(doctor, today, booking.time());
            if (slot == null) {
                continue;
            }

            var appointment = appointment(booking.patient(), doctor, slot,
                    AppointmentStatus.UPCOMING);
            if (booking.formIn()) {
                appointment.setFormSubmittedAt(OffsetDateTime.now().minusDays(1));
            }
            if (booking.arrived()) {
                // Long enough ago that the queue shows a real wait.
                appointment.setCheckedInAt(OffsetDateTime.now().minusMinutes(
                        12L + todays.size() * 7L));
            }
            todays.add(appointment);
        }
        return todays;
    }

    /** The slot at one time on one day, whether or not it is still free. */
    private ScheduleTime findSlotOn(Doctor doctor, LocalDate date, LocalTime time) {
        return scheduleTimeRepository.findAll().stream()
                .filter(slot -> slot.getSchedule() != null)
                .filter(slot -> date.equals(slot.getSchedule().getDate()))
                .filter(slot -> doctor.getId().equals(slot.getSchedule().getDoctor().getId()))
                .filter(slot -> time.equals(slot.getTime()))
                .findFirst()
                .orElse(null);
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

    /**
     * Gives the demo patient an allergy, a medication and a condition.
     *
     * Without these the patient summary is three empty sections, which is
     * valid PS-CA but shows nothing of what the export is for.
     */
    private void seedHealthRecord(Patient patient) {
        var entries = List.of(
                healthEntry(patient, HealthEntryType.ALLERGY, "Penicillin",
                        "Rash and swelling within an hour of the first dose.",
                        LocalDate.of(2012, 6, 1)),
                healthEntry(patient, HealthEntryType.ALLERGY, "Peanuts",
                        "Mild — itching in the mouth.", null),
                healthEntry(patient, HealthEntryType.MEDICATION, "Metformin 500mg",
                        "Twice a day with meals.", LocalDate.of(2023, 2, 14)),
                healthEntry(patient, HealthEntryType.MEDICATION, "Vitamin D 1000 IU",
                        "Once daily through the winter.", null),
                healthEntry(patient, HealthEntryType.CONDITION, "Type 2 diabetes",
                        "Managed with medication and diet.", LocalDate.of(2023, 2, 1)),
                healthEntry(patient, HealthEntryType.CONDITION, "Asthma",
                        "Exercise-induced; inhaler as needed.", LocalDate.of(2008, 9, 1)));

        healthEntryRepository.saveAll(entries);
        log.info("Demo data: seeded {} health record entries", entries.size());
    }

    private com.vegs.mediconnect.datasource.health.HealthEntry healthEntry(
            Patient patient, HealthEntryType type, String description,
            String note, LocalDate onset) {
        var entry = new com.vegs.mediconnect.datasource.health.HealthEntry();
        entry.setPatient(patient);
        entry.setType(type);
        entry.setDescription(description);
        entry.setNote(note);
        entry.setOnsetDate(onset);
        entry.setActive(true);
        return entry;
    }

    /**
     * Answers for every visit the seed marks as "form sent".
     *
     * Marking a form as sent without any answers behind it showed the
     * patient "Sent yesterday" and then an empty form when they opened it,
     * which looked exactly like their answers had been lost.
     */
    private void seedForms(List<Appointment> appointments) {
        String[][] answers = {
                {"Follow-up on my blood pressure readings", "fatigue,headache", "NO", "NO", "YES",
                        "Readings have been higher in the mornings."},
                {"A cough that has not cleared in three weeks", "cough", "NO", "NO", "NO", null},
                {"Annual check-up", "none", "NO", "NO", "UNSURE", null},
                {"Pain in my right knee when climbing stairs", "none", "YES", "NO", "NO",
                        "Arthroscopy on the same knee in March."},
        };
        var forms = new ArrayList<com.vegs.mediconnect.datasource.previsit.PreVisitForm>();
        int next = 0;
        for (Appointment appointment : appointments) {
            if (appointment.getFormSubmittedAt() == null) {
                continue;
            }
            String[] a = answers[next++ % answers.length];
            var form = new com.vegs.mediconnect.datasource.previsit.PreVisitForm();
            form.setAppointment(appointment);
            form.setReason(a[0]);
            form.setSymptoms(a[1]);
            form.setHadSurgery(a[2]);
            form.setSmokes(a[3]);
            form.setDrinksAlcohol(a[4]);
            form.setNotes(a[5]);
            forms.add(form);
        }
        preVisitFormRepository.saveAll(forms);
    }

    /**
     * The demo patient waiting for something earlier with a doctor they
     * already see, with a slot being held for them right now — so Visits
     * shows its offer banner and Messages its "See offer" straight away.
     */
    private void seedWaitlistOffer(Patient patient, Doctor doctor, List<Appointment> appointments) {
        Appointment later = appointments.stream()
                .filter(appointment -> appointment.getPatient().equals(patient))
                .filter(appointment -> appointment.getDoctor().equals(doctor))
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getDateTime().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);
        if (later == null) {
            return;
        }

        var entry = new com.vegs.mediconnect.datasource.waitlist.WaitlistEntry();
        entry.setPatient(patient);
        entry.setDoctor(doctor);
        entry.setCurrentAppointmentDate(later.getScheduleTime().getSchedule().getDate());
        entry.setStatus(com.vegs.mediconnect.datasource.waitlist.WaitlistStatus.WAITING);
        waitlistEntryRepository.save(entry);

        // The first free slot with this doctor that is earlier than theirs
        // and far enough ahead to be held.
        scheduleTimeRepository.findAll().stream()
                .filter(slot -> slot.getSchedule() != null)
                .filter(slot -> doctor.equals(slot.getSchedule().getDoctor()))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                .filter(slot -> slot.getDateTime().isBefore(later.getDateTime()))
                .filter(slot -> slot.getSchedule().getDate().isBefore(entry.getCurrentAppointmentDate()))
                .filter(slot -> com.vegs.mediconnect.mobile.waitlist.WaitlistService
                        .holdEnds(slot, OffsetDateTime.now()) != null)
                .min(java.util.Comparator.comparing(ScheduleTime::getDateTime))
                .ifPresent(slot -> waitlistService.offerSlot(slot, null));
    }

    private void seedNotifications(List<Patient> patients, List<Appointment> appointments) {
        // A reminder is about one patient's visit, so it goes to that
        // patient alone and carries the visit, which is what lets the
        // message offer "Fill in form" instead of saying where to find it.
        Patient demoPatient = patients.getFirst();
        appointments.stream()
                .filter(appointment -> appointment.getPatient().equals(demoPatient))
                .filter(appointment -> appointment.getBookedForName() == null)
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getFormSubmittedAt() == null)
                .filter(appointment -> appointment.getDateTime().toLocalDate().isAfter(LocalDate.now()))
                .min(java.util.Comparator.comparing(Appointment::getDateTime))
                .ifPresent(visit -> {
                    var reminder = notification(NotificationKind.APPOINTMENT, "Appointment reminder",
                            "You have an appointment with Dr. " + visit.getDoctor().getLastName()
                                    + " on " + visit.getDateTime().format(
                                    java.time.format.DateTimeFormatter.ofPattern(
                                            "EEEE, d MMMM", java.util.Locale.ENGLISH))
                                    + ". Please complete your pre-appointment form beforehand.");
                    reminder.setSendAllPatients(false);
                    reminder.setAppointmentId(visit.getId());
                    var saved = notificationRepository.save(reminder);
                    var link = new NotificationPatient();
                    link.setNotificationId(saved);
                    link.setPatientId(demoPatient);
                    link.setAcknowledged(false);
                    notificationPatientRepository.save(link);
                });

        var notifications = List.of(
                notification(NotificationKind.ANNOUNCEMENT, "Flu shots available",
                        "Walk-in flu vaccinations are available at the clinic every weekday "
                                + "between 9am and 4pm, no appointment needed."),
                notification(NotificationKind.ANNOUNCEMENT, "Clinic hours over the holidays",
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

    private Notification notification(String kind, String title, String message) {
        var notification = new Notification();
        notification.setKind(kind);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSendAllPatients(true);
        notification.setIsDeleted(false);
        return notification;
    }
}
