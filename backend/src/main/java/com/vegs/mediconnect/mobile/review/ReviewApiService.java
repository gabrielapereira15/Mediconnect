package com.vegs.mediconnect.mobile.review;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.review.Review;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.mobile.appointment.AppointmentNotFoundException;
import com.vegs.mediconnect.mobile.review.model.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewApiService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * A review of one of the signed-in patient's own visits.
     *
     * Any appointment id used to be accepted, so anybody could rate any
     * doctor as often as they liked, on visits that were not theirs or had
     * not happened. Now the visit must be the patient's (somebody else's is
     * reported as missing, so ids cannot be probed), must have taken place
     * and not been cancelled, and takes one review.
     */
    @Transactional
    public void createReview(ReviewRequest reviewRequest, String requestingEmail) {
        Appointment appointment = appointmentRepository
                .findById(reviewRequest.getAppointmentId())
                .filter(found -> requestingEmail != null
                        && requestingEmail.equalsIgnoreCase(found.getPatient().getEmail()))
                .orElseThrow(AppointmentNotFoundException::new);

        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            throw new ReviewNotAllowedException("A cancelled visit cannot be reviewed.");
        }
        if (!appointment.getDateTime().isBefore(java.time.LocalDateTime.now())) {
            throw new ReviewNotAllowedException("You can review a visit once it has happened.");
        }
        if (reviewRepository.findByAppointment(appointment).isPresent()) {
            throw new ReviewNotAllowedException("You have already reviewed this visit.");
        }

        var review = new Review();
        review.setAppointment(appointment);
        review.setScore(reviewRequest.getScore());
        review.setDoctor(appointment.getDoctor());
        String description = reviewRequest.getDescription();
        review.setDescription(description == null || description.isBlank() ? null : description.trim());
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            // Two taps that both got past the check above: the table allows
            // one review per visit, and the second is told so.
            throw new ReviewNotAllowedException("You have already reviewed this visit.");
        }
    }

}
