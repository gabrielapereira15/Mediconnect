package com.vegs.mediconnect.backoffice.patient;

import java.util.List;
import java.util.UUID;

/**
 * One patient's chart, as a clinician reads it (board B04).
 *
 * Allergies are pulled out on their own because they are the one thing
 * here that has to be seen before anything else; everything else is what
 * the patient has told the clinic, through the app, in their own words.
 */
public record PatientChart(
        UUID id,
        String name,
        String initials,
        String avatarClass,
        String demographics,
        String phone,
        String email,
        String address,
        String healthCard,
        List<Allergy> allergies,
        List<Entry> entries,
        List<Visit> upcoming,
        List<Visit> past,
        List<Form> forms,
        int appointmentCount) {

    public record Allergy(String description, String note) {
    }

    /** One line of the health record, with whether it is coded or free text. */
    public record Entry(String typeLabel, String badgeClass, String description, String note,
                       String since, String coding, boolean coded, boolean active) {
    }

    public record Visit(UUID id, String weekday, String dayOfMonth, String title, String sub,
                        String status, String statusClass, String link) {
    }

    public record Form(String heading, List<Answer> answers) {
    }

    public record Answer(String question, String answer) {
    }

    public Form latestForm() {
        return forms.isEmpty() ? null : forms.getFirst();
    }

    public int activeEntryCount() {
        return (int) entries.stream().filter(Entry::active).count();
    }
}
