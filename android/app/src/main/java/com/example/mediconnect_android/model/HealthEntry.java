package com.example.mediconnect_android.model;

/**
 * One line of the patient's health record.
 *
 * The three types mirror the three sections a pan-Canadian Patient Summary
 * must contain, which is why they are the three the app collects.
 */
public class HealthEntry {

    public static final String TYPE_ALLERGY = "ALLERGY";
    public static final String TYPE_MEDICATION = "MEDICATION";
    public static final String TYPE_CONDITION = "CONDITION";

    private String id;
    private String type;
    private String description;
    private String note;
    private String onsetDate;
    private boolean active;

    /** True once a clinician has attached a clinical code to it. */
    private boolean coded;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOnsetDate() {
        return onsetDate;
    }

    public void setOnsetDate(String onsetDate) {
        this.onsetDate = onsetDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCoded() {
        return coded;
    }

    public void setCoded(boolean coded) {
        this.coded = coded;
    }
}
