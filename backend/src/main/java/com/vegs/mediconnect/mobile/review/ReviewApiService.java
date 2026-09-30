package com.vegs.mediconnect.mobile.review;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.review.Review;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.mobile.appointment.AppointmentNotFoundException;
import com.vegs.mediconnect.mobile.review.model.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewApiService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public void createReview(ReviewRequest reviewRequest) {
        Appointment appointment = appointmentRepository
                .findById(reviewRequest.getAppointmentId())
                .orElseThrow(AppointmentNotFoundException::new);

        var review = new Review();
        review.setAppointment(appointment);
        review.setScore(reviewRequest.getScore());
        review.setDoctor(appointment.getDoctor());
        review.setDescription(reviewRequest.getDescription());
        reviewRepository.save(review);
    }

}
