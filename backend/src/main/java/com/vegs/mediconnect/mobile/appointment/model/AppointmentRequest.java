package com.vegs.mediconnect.mobile.appointment.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AppointmentRequest {

    @NotNull
    private String patientEmail;
    @NotNull
    private UUID scheduleTimeId;

    /**
     * Only sent when booking on someone else's behalf. Absent means the
     * appointment is for the account holder, which is the common case.
     */
    private String bookedForName;

    /** ISO yyyy-MM-dd. Display formats are not safe to parse. */
    private String bookedForDateOfBirth;

    private String bookedForPhone;
    private String bookedForNotes;

}
