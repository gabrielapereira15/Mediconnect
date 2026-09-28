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

}
