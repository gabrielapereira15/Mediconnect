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
     * The day of the appointment they already hold, or of the full day they
     * wanted when they hold none. The server works out which from their
     * bookings: a slot earlier than a held visit is worth offering, and so
     * is one on or before a day they asked for.
     */
    @NotNull
    private LocalDate currentAppointmentDate;

    /** The earliest date they could actually attend. Optional. */
    private LocalDate availableFrom;
}
