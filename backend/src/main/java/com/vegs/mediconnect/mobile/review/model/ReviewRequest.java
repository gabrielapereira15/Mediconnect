package com.vegs.mediconnect.mobile.review.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequest {

    @NotNull
    private UUID appointmentId;
    /** The app's five stars: whole or half, never outside 1 to 5. */
    @NotNull
    @DecimalMin("1")
    @DecimalMax("5")
    private Float score;
    @Size(max = 1000)
    private String description;

}
