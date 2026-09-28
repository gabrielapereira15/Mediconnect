package com.vegs.mediconnect.mobile.appointment.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AppointmentRequest {

    @NotNull
    private String patientEmail;
    @NotNull
    private UUID scheduleTimeId;

}
