package com.vegs.mediconnect.mobile.health.model;

import com.vegs.mediconnect.datasource.health.HealthEntryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class HealthEntryResponse {

    private UUID id;
    private HealthEntryType type;
    private String description;
    private String note;
    private String onsetDate;
    private boolean active;

    /**
     * True once a clinician has attached a clinical code to the patient's
     * own words. The app shows this so a patient can see their entry has
     * been reviewed rather than just recorded.
     */
    private boolean coded;
}
