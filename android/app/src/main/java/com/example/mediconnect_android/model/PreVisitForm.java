package com.example.mediconnect_android.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The answers to the pre-appointment form.
 *
 * The same shape goes up and comes back, so a patient who reopens the form
 * sees what they sent rather than an empty one.
 */
public class PreVisitForm {

    private String reason;
    private List<String> symptoms = new ArrayList<>();
    private String hadSurgery;
    private String smokes;
    private String drinksAlcohol;
    private String notes;
    private String submittedAt;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getSymptoms() {
        return symptoms == null ? new ArrayList<>() : symptoms;
    }

    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms;
    }

    public String getHadSurgery() {
        return hadSurgery;
    }

    public void setHadSurgery(String hadSurgery) {
        this.hadSurgery = hadSurgery;
    }

    public String getSmokes() {
        return smokes;
    }

    public void setSmokes(String smokes) {
        this.smokes = smokes;
    }

    public String getDrinksAlcohol() {
        return drinksAlcohol;
    }

    public void setDrinksAlcohol(String drinksAlcohol) {
        this.drinksAlcohol = drinksAlcohol;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }
}
