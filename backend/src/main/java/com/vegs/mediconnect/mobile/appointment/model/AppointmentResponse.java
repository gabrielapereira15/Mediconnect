package com.vegs.mediconnect.mobile.appointment.model;

import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AppointmentResponse {

    private UUID id;
    private String status;
    private String date;
    private String time;
    private boolean isReviewed;
    private DoctorSimpleResponse doctor;

}
