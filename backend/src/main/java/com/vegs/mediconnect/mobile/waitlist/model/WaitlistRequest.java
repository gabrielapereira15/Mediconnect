package com.vegs.mediconnect.mobile.waitlist.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class WaitlistRequest {

    @NotNull
    private UUID doctorId;

    /**
     * The appointment they already hold. An offer is only worth making when
     * the freed slot is earlier than this.
     */
    @NotNull
    private LocalDate currentAppointmentDate;

    /** The earliest date they could actually attend. Optional. */
    private LocalDate availableFrom;
}
