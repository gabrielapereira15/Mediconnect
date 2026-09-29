package com.vegs.mediconnect.mobile.previsit.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** The answers the app collected, as they arrive. */
@Getter
@Setter
public class PreVisitFormRequest {

    private String reason;

    /** The app's own symptom codes, not the labels the patient read. */
    private List<String> symptoms;

    private String hadSurgery;
    private String smokes;
    private String drinksAlcohol;
    private String notes;
}
