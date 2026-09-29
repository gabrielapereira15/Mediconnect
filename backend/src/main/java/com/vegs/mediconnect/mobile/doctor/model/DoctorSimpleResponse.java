package com.vegs.mediconnect.mobile.doctor.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class DoctorSimpleResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String name;
    private String specialty;
    private Float score;
    private Integer reviewCount;
    private String photo;

    /** Years in practice, shown beside the specialty on a doctor card. */
    private String experienceYears;

    /**
     * When this doctor is next free, as an ISO-8601 local date-time, or null
     * when they have nothing open in the booking window.
     *
     * A timestamp rather than a formatted string: the card says "Today 2:30
     * PM" or "Next: Thu 1 Oct" depending on the day, and that decision
     * belongs to whatever is drawing the card.
     */
    private String nextAvailableAt;

}
