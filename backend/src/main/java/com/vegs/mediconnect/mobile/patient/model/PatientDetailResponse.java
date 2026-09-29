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

    /**
     * The provincial health card. In FHIR this becomes the jurisdictional
     * health number CA Baseline and CA Core+ slice Patient.identifier on, so
     * without it a patient is only identifiable inside this clinic.
     */
    private String healthCardNumber;

    /** Two-letter province code; the identifier system differs per province. */
    private String healthCardProvince;

}
