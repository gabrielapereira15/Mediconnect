package com.vegs.mediconnect.mobile.review;

import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import com.vegs.mediconnect.mobile.review.model.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/mobile/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewApiService reviewApiService;

    @PostMapping
    public ResponseEntity<Void> createReview(@RequestBody ReviewRequest reviewRequest) {
        reviewApiService.createReview(reviewRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

}
