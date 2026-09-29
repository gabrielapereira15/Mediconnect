package com.vegs.mediconnect.mobile.doctor;

import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import com.vegs.mediconnect.mobile.review.model.ReviewDTO;
import com.vegs.mediconnect.mobile.schedule.model.ScheduleResponse;
import com.vegs.mediconnect.mobile.schedule.model.ScheduleTimeResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.*;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class DoctorApiService {

    private final DoctorRepository doctorRepository;
    private final ReviewRepository reviewRepository;

    @Value("${mediconnect.photo-baseurl}")
    private String photoBaseulr;

    /**
     * The doctor directory, soonest free first.
     *
     * It used to sort by score ascending, so the directory opened on the
     * lowest-rated doctor and the best one was last. A patient looking for
     * an appointment is choosing on when they can be seen, so that is the
     * order; rating breaks ties, highest first, and anyone with nothing free
     * sits at the end rather than being hidden.
     *
     * Transactional because building each row walks the doctor's schedules,
     * which are lazy.
     */
    @Transactional
    public List<DoctorSimpleResponse> getDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::mapToDoctorSimpleResponse)
                .sorted(Comparator
                        .comparing(DoctorApiService::nextAvailableOrFarFuture)
                        .thenComparing(
                                doctor -> Optional.ofNullable(doctor.getScore()).orElse(0.0f),
                                Comparator.reverseOrder()))
                .toList();
    }

    /** Doctors with nothing free sort last rather than first. */
    private static LocalDateTime nextAvailableOrFarFuture(DoctorSimpleResponse doctor) {
        return doctor.getNextAvailableAt() == null
                ? LocalDateTime.MAX
                : LocalDateTime.parse(doctor.getNextAvailableAt());
    }

    public Doctor getDoctor(UUID doctorId) {
        var optDoctor = doctorRepository.findById(doctorId);
        if (optDoctor.isEmpty()) {
            throw new DoctorNotFoundException();
        }
        return optDoctor.get();
    }

    @Transactional
    public DoctorResponse getDoctorDetails(UUID doctorId) {
        var optDoctor = doctorRepository.findById(doctorId);
        if (optDoctor.isEmpty()) {
            throw new DoctorNotFoundException();
        }

        var doctor = optDoctor.get();
        var dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
        var timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        var scheduleResponses = new ArrayList<ScheduleResponse>();
        doctor
                .getSchedules()
                .stream()
                .filter(DoctorApiService::isTodayOrLate)
                .sorted(Comparator.comparing(Schedule::getDate))
                .forEach(schedule -> addIfApplicable(schedule, dateFormat, timeFormat, scheduleResponses));

        var randomReview = calculateReview(doctor);

        return DoctorResponse
                .builder()
                .name(doctor.getFullName())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .photo(getPhotoUrl(doctor))
                .description(doctor.getAbout())
                .experienceYears(doctor.getExperienceInYears())
                .score(randomReview.score())
                .reviewCount(randomReview.count())
                .schedule(scheduleResponses)
                .build();
    }

    private void addIfApplicable(Schedule schedule,
                                 DateTimeFormatter dateFormat, DateTimeFormatter timeFormat,
                                 List<ScheduleResponse> scheduleResponses) {
        var times = getTimes(schedule, timeFormat);
        if (!times.isEmpty()) {
            scheduleResponses.add(ScheduleResponse
                    .builder()
                    .date(schedule.getDate().format(dateFormat))
                    .times(times)
                    .build());
        }
    }

    private static boolean isTodayOrLate(Schedule schedule) {
        return !LocalDate.now().isAfter(schedule.getDate());
    }

    public DoctorSimpleResponse mapToDoctorSimpleResponse(Doctor doctor) {
        var review = calculateReview(doctor);
        var nextFree = nextAvailableSlot(doctor);

        return DoctorSimpleResponse
                .builder()
                .id(doctor.getId())
                .photo(getPhotoUrl(doctor))
                .specialty(doctor.getSpecialty())
                .name(doctor.getFullName())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .experienceYears(doctor.getExperienceInYears())
                .score(review.score())
                .reviewCount(review.count())
                .nextAvailableAt(nextFree
                        .map(slot -> slot.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .orElse(null))
                .build();
    }

    /**
     * The earliest slot this doctor still has open.
     *
     * Only slots that are both free and actually still ahead of us: a
     * morning slot on today's schedule is no use at four in the afternoon,
     * and offering it is how a directory loses a patient's trust on their
     * first tap.
     */
    private Optional<LocalDateTime> nextAvailableSlot(Doctor doctor) {
        LocalDateTime now = LocalDateTime.now();

        return doctor.getSchedules().stream()
                .filter(schedule -> !LocalDate.now().isAfter(schedule.getDate()))
                .flatMap(schedule -> schedule.getScheduleTimes().stream()
                        .filter(time -> Boolean.TRUE.equals(time.getAvailable()))
                        .map(time -> LocalDateTime.of(schedule.getDate(), time.getTime())))
                .filter(slot -> slot.isAfter(now))
                .min(Comparator.naturalOrder());
    }

    private List<ScheduleTimeResponse> getTimes(Schedule schedule, DateTimeFormatter timeFormat) {
        return schedule
                .getScheduleTimes()
                .stream()
                .filter(ScheduleTime::getAvailable)
                .filter(this::isAtLeastSixHoursAhead)
                .sorted(Comparator.comparing(ScheduleTime::getTime))
                .map(scheduleTime -> ScheduleTimeResponse
                        .builder()
                        .id(scheduleTime.getId())
                        .time(scheduleTime.getTime().format(timeFormat))
                        .build())
                .toList();
    }

    private boolean isAtLeastSixHoursAhead(ScheduleTime scheduleTime) {
        return scheduleTime
                .getDateTime()
                .isAfter(LocalDateTime
                        .now()
                        .plusHours(6));
    }

    private String getPhotoUrl(Doctor doctor) {
        return format("%s/api/mobile/doctors/photo/%s", photoBaseulr, doctor.getId().toString());
    }

    private ReviewDTO calculateReview(Doctor doctor) {
        var reviews = reviewRepository
                .findAllByDoctor(doctor);

        if (reviews.isEmpty()) {
            return new ReviewDTO(null, null);
        }

        Integer reviewCount = reviews.size();
        Float total = 0f;
        for (var review : reviews) {
            total += review.getScore();
        }
        float score = total/reviewCount;
        score = Math.round(score * 10) / 10.0f;

        return new ReviewDTO(reviewCount, score);
    }

}
