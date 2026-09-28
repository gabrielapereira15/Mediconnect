package com.vegs.mediconnect.backoffice.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;


@Getter
@Setter
public class ScheduleDTO {

    private UUID id;

    @NotNull
    private LocalDate date;

    @NotNull
    private UUID doctorId;

    @NotNull
    @DateTimeFormat(pattern = "h a")
    private Set<LocalTime> times;

    private String doctorName;

    @ScheduleDoctorDateUnique
    public DoctorDateDTO getDoctorDate() {
        return new DoctorDateDTO(date, doctorId);
    }
}
