package com.vegs.mediconnect.backoffice.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
public class AppointmentDTO {

    private UUID id;

    @Size(max = 255)
    private String status;

    private Boolean cancelled;
    private boolean removed;

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID doctorId;

    private UUID scheduleTimeId;

    @NotNull
    private LocalDateTime scheduleTime;

    private String patientName;
    private String doctorName;
    private String bookTime;

    public boolean notRemoved() {
        return !removed;
    }

}
