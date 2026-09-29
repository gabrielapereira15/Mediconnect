package com.vegs.mediconnect.mobile.health.model;

import com.vegs.mediconnect.datasource.health.HealthEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthEntryRequest {

    @NotNull
    private HealthEntryType type;

    @NotBlank
    private String description;

    /** For allergies, what happens. For conditions, how it presents. */
    private String note;

    /** ISO yyyy-MM-dd, or absent when the patient does not remember. */
    private String onsetDate;
}
