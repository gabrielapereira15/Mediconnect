package com.vegs.mediconnect.mobile.waitlist.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class WaitlistResponse {

    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String status;
    private String currentAppointmentDate;
    private String availableFrom;
}
