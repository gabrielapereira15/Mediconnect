package com.vegs.mediconnect.datasource.staff;

/**
 * What a member of staff may do in the back office.
 *
 * Two roles, because the clinic has two jobs to do here. The front desk
 * books, checks in and cancels; a clinician reads what the patient sent
 * ahead. Both see the day; only one of them sees a patient's record.
 */
public enum StaffRole {

    FRONT_DESK("Front desk"),
    CLINICIAN("Clinician");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Initials for the avatar in the side navigation. */
    public String getInitials() {
        return this == FRONT_DESK ? "FD" : "DR";
    }
}
