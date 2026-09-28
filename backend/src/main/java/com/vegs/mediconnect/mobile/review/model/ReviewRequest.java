package com.vegs.mediconnect.mobile.review.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequest {

    @NotNull
    private UUID appointmentId;
    @NotNull
    private Float score;
    private String description;

}
