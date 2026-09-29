package com.vegs.mediconnect.mobile.previsit.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** The answers as stored, so the app can show them again. */
@Getter
@Setter
@Builder
public class PreVisitFormResponse {

    private String reason;
    private List<String> symptoms;
    private String hadSurgery;
    private String smokes;
    private String drinksAlcohol;
    private String notes;

    /** ISO local date-time the form arrived. */
    private String submittedAt;
}
