package com.vegs.mediconnect.backoffice.schedule_time;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;


@Getter
@Setter
public class ScheduleTimeDTO {

    private UUID id;

    @NotNull
    @DateTimeFormat(pattern = "HH:mm a")
    private LocalTime time;

    @NotNull
    private UUID scheduleId;

    private LocalDate scheduleDate;

    private String doctorName;

    public ScheduleTimeDTO() {}

    public ScheduleTimeDTO(LocalTime localTime) {
        this.time = localTime;
    }

    @ScheduleDateTimeUnique
    public ScheduleDateTimeDTO getScheduleDateTime() {
        return new ScheduleDateTimeDTO(time, scheduleId);
    }

}
