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
import java.util.*;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class DoctorApiService {

    private final DoctorRepository doctorRepository;
    private final ReviewRepository reviewRepository;

    @Value("${vegs.photo-baseurl}")
    private String photoBaseulr;

    public List<DoctorSimpleResponse> getDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::mapToDoctorSimpleResponse)
                .sorted(Comparator.comparing(
                        doctor -> Optional.ofNullable(doctor.getScore()).orElse(0.0f))
                )
                .toList();
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
        var dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM");
        var timeFormat = DateTimeFormatter.ofPattern("h a");
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
        var randomReview = calculateReview(doctor);
        return DoctorSimpleResponse
                .builder()
                .id(doctor.getId())
                .photo(getPhotoUrl(doctor))
                .specialty(doctor.getSpecialty())
                .name(doctor.getFullName())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .score(randomReview.score())
                .reviewCount(randomReview.count())
                .build();
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
