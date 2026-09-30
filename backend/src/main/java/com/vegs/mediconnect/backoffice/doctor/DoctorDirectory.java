package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.review.Review;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The clinic's doctors as the back office shows and edits them (board
 * B06): a card each with how full this week is, and the profile patients
 * read in the app.
 */
@Service
@RequiredArgsConstructor
public class DoctorDirectory {

    /** Big enough for a clear headshot, small enough for a phone to fetch. */
    static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final DoctorRepository doctorRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;

    public record Card(UUID id, String name, String initials, String avatarClass, String specialty,
                       String experience, int percentBooked, boolean hasPhoto) {
    }

    public record Profile(UUID id, String firstName, String lastName, String specialty,
                          String experienceInYears, String about, String name, String initials,
                          String avatarClass, boolean hasPhoto) {
    }

    public record ReviewLine(String stars, int score, String text, String when, String by) {
    }

    @Transactional(readOnly = true)
    public List<Card> cards() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        Map<UUID, int[]> load = new HashMap<>();
        Set<UUID> booked = new java.util.HashSet<>();
        for (Appointment appointment : appointmentRepository.findAllBetween(monday, sunday)) {
            if (AvailabilityService.active(appointment)) {
                booked.add(appointment.getScheduleTime().getId());
            }
        }
        for (ScheduleTime slot : scheduleTimeRepository.findAllBetween(monday, sunday)) {
            if (Boolean.TRUE.equals(slot.getBlocked())) {
                continue;
            }
            int[] count = load.computeIfAbsent(slot.getSchedule().getDoctor().getId(), key -> new int[2]);
            count[1]++;
            if (booked.contains(slot.getId())) {
                count[0]++;
            }
        }

        return doctorRepository.findAll(Sort.by("lastName", "firstName")).stream()
                .map(doctor -> {
                    String name = Display.name(doctor.getFirstName(), doctor.getLastName());
                    int[] count = load.getOrDefault(doctor.getId(), new int[2]);
                    int percent = count[1] == 0 ? 0 : Math.round(count[0] * 100f / count[1]);
                    return new Card(doctor.getId(), "Dr. " + name, Display.initials(name),
                            Display.avatarClass(name), doctor.getSpecialty(),
                            doctor.getExperienceInYears(), percent,
                            doctor.getProfilePhoto() != null && doctor.getProfilePhoto().length > 0);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Profile profile(UUID id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(NotFoundException::new);
        String name = Display.name(doctor.getFirstName(), doctor.getLastName());
        return new Profile(doctor.getId(), doctor.getFirstName(), doctor.getLastName(), doctor.getSpecialty(),
                doctor.getExperienceInYears(), doctor.getAbout(), "Dr. " + name, Display.initials(name),
                Display.avatarClass(name), doctor.getProfilePhoto() != null && doctor.getProfilePhoto().length > 0);
    }

    @Transactional(readOnly = true)
    public List<ReviewLine> reviews(UUID id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(NotFoundException::new);
        return reviewRepository.findAllByDoctor(doctor).stream()
                .sorted(Comparator.comparing(Review::getDateCreated,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(review -> {
                    int score = review.getScore() == null ? 0 : Math.round(review.getScore());
                    var patient = review.getAppointment() == null ? null : review.getAppointment().getPatient();
                    // A first name and an initial: enough to be a person,
                    // not enough to identify one.
                    String by = patient == null ? "A patient"
                            : patient.getFirstName() + (patient.getLastName() == null || patient.getLastName().isEmpty()
                            ? "" : " " + patient.getLastName().charAt(0) + ".");
                    return new ReviewLine("★".repeat(score) + "☆".repeat(Math.max(0, 5 - score)),
                            score, review.getDescription(),
                            review.getDateCreated() == null ? "" : Display.day(Display.local(review.getDateCreated()).toLocalDate()),
                            by);
                })
                .toList();
    }

    /** Saves what patients read about a doctor. Returns the problem, or null. */
    @Transactional
    public String update(UUID id, String firstName, String lastName, String specialty,
                         String experience, String about) {
        String problem = checkProfile(firstName, lastName, specialty, experience);
        if (problem != null) {
            return problem;
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow(NotFoundException::new);
        apply(doctor, firstName, lastName, specialty, experience, about);
        doctorRepository.save(doctor);
        return null;
    }

    /** A new doctor, with no hours yet: the Availability tab comes next. */
    @Transactional
    public Doctor create(String firstName, String lastName, String specialty, String experience, String about) {
        String problem = checkProfile(firstName, lastName, specialty, experience);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        var doctor = new Doctor();
        apply(doctor, firstName, lastName, specialty, experience, about);
        return doctorRepository.save(doctor);
    }

    /**
     * Replaces the photo patients see.
     *
     * Checked by what the file actually is, not by what the browser says
     * it is: the photo is served to every copy of the app, so a file that
     * only claims to be a JPEG does not get stored.
     *
     * @return the problem, or null once saved
     */
    @Transactional
    public String updatePhoto(UUID id, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "Choose a photo to upload.";
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            return "That photo is larger than 2 MB. A smaller one loads faster in the app.";
        }
        byte[] bytes = file.getBytes();
        String type = sniff(bytes);
        if (type == null || !PHOTO_TYPES.contains(type)) {
            return "Use a JPEG, PNG or WebP photo.";
        }
        Doctor doctor = doctorRepository.findById(id).orElseThrow(NotFoundException::new);
        doctor.setProfilePhoto(bytes);
        doctor.setProfilePhotoExtension(type);
        doctorRepository.save(doctor);
        return null;
    }

    /** The image type from the file's first bytes, or null if it is not one we take. */
    static String sniff(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static String checkProfile(String firstName, String lastName, String specialty, String experience) {
        if (blank(firstName) || blank(lastName)) {
            return "A doctor needs a first and a last name.";
        }
        if (blank(specialty)) {
            return "Say what the doctor's specialty is; patients filter by it.";
        }
        if (!blank(experience) && !experience.trim().matches("\\d{1,2}")) {
            return "Years of experience is a number, such as 12.";
        }
        return null;
    }

    private static void apply(Doctor doctor, String firstName, String lastName, String specialty,
                              String experience, String about) {
        doctor.setFirstName(firstName.trim());
        doctor.setLastName(lastName.trim());
        doctor.setSpecialty(specialty.trim());
        doctor.setExperienceInYears(blank(experience) ? null : experience.trim());
        doctor.setAbout(blank(about) ? null : about.trim());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
