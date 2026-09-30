package com.vegs.mediconnect.mobile.appointment.model;

import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AppointmentResponse {

    private UUID id;
    private String status;
    private String date;
    private String time;

    /**
     * ISO-8601 local date-time, e.g. 2026-09-30T10:00:00.
     *
     * `date` and `time` are formatted for display and are not safe to parse:
     * they change with the display format and the locale. Anything that needs
     * the actual moment — scheduling a reminder, sorting — uses this.
     */
    private String startsAt;
    private boolean isReviewed;

    /**
     * The score the patient gave, 0.5-5.0, or null if they have not reviewed
     * this visit. Without it the app can only hide the "Add review" button,
     * which reads as the option having vanished rather than being done.
     */
    private Float reviewScore;

    /**
     * Who the visit is for, when that is not the account holder. Null means
     * the patient booked it for themselves.
     */
    private String bookedForName;

    /** ISO local date-time the patient checked in, or null. */
    private String checkedInAt;

    /** ISO local date-time the pre-appointment form arrived, or null. */
    private String formSubmittedAt;

    /** When the patient said they will be there; null until they do. */
    private String attendanceConfirmedAt;

    private DoctorSimpleResponse doctor;

}
