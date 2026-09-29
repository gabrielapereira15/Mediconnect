package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.PreVisitForm;

/** The pre-appointment form for one visit. */
public interface PreVisitFormClient {

    /** Sends the answers; returns them as stored, or null if it failed. */
    PreVisitForm submit(String appointmentId, PreVisitForm form);

    /** The answers already sent, or null if none have been. */
    PreVisitForm get(String appointmentId);
}
