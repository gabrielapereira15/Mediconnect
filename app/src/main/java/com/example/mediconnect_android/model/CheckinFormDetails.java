package com.example.mediconnect_android.model;

import java.util.List;

record CheckinFormDetails(String primaryReason,
                          String takingMedication,
                          List<String> recentlySymptoms,
                          String additionalNotes) {}