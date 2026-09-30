package com.example.mediconnect_android.model;

import java.util.List;

record PreAppointmentFormDetails(Boolean hasSurgery,
                                 Boolean smoke,
                                 Boolean alcohol,
                                 List<String> chronicConditions,
                                 List<String> familyHistory) {
}