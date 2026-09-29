package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.HealthEntry;

import java.util.List;

public interface HealthClient {

    List<HealthEntry> getEntries(String email);

    HealthEntry addEntry(String email, String type, String description,
                         String note, String onsetDate);

    /** Marks an entry no longer current. Nothing is deleted. */
    boolean stopEntry(String email, String entryId);
}
