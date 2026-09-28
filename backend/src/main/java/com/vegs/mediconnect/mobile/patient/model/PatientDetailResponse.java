package com.vegs.mediconnect.mobile.patient.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class PatientDetailResponse {

    private UUID id;
    private String email;
    private String clinicCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String gender;
    private String birthdate;
    private String phoneNumber;
    private String address;

}
