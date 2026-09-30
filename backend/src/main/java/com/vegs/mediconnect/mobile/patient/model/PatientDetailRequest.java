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
     *
     * Left out, the card already on file is kept; blank, it is removed. It
     * is not @NotNull because the card is optional, and not a bean
     * validation pattern because {@code HealthCard} explains a bad one in
     * words the app can show.
     */
    private String healthCardNumber;

    /** Two-letter province code; the identifier system differs per province. */
    private String healthCardProvince;

}
