package com.vegs.mediconnect.mobile.patient.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PatientDetailRequest {

    @NotNull
    private String email;
    @NotNull
    private String clinicCode;
    @NotNull
    private String firstName;
    @NotNull
    private String lastName;
    @NotNull
    private String gender;
    @NotNull
    private String birthdate;
    @NotNull
    private String phoneNumber;
    @NotNull
    private String address;

}
