package com.vegs.mediconnect.backoffice.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class DoctorDateDTO {

    private LocalDate date;

    private UUID doctorId;

}
