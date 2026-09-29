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

    /**
     * The provincial health card. In FHIR this becomes the jurisdictional
     * health number CA Baseline and CA Core+ slice Patient.identifier on, so
     * without it a patient is only identifiable inside this clinic.
     */
    private String healthCardNumber;

    /** Two-letter province code; the identifier system differs per province. */
    private String healthCardProvince;

}
