package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.HealthEntry;

import java.util.List;

public interface HealthClient {

    List<HealthEntry> getEntries(String email);

    HealthEntry addEntry(String email, String type, String description,
                         String note, String onsetDate);

    /** Marks an entry no longer current. Nothing is deleted. */
    boolean stopEntry(String email, String entryId);

    /**
     * The patient summary as a PS-CA FHIR document, exactly as the server
     * produced it. Returned as raw JSON because its value is being a
     * standard document another system can read, not a shape this app
     * happens to parse.
     */
    String getSummaryDocument(String patientId);
}
