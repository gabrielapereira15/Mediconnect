package com.vegs.mediconnect.backoffice.appointment;

import java.util.List;
import java.util.UUID;

/**
 * One appointment, as the side panel shows it (board B03).
 *
 * The allergies sit near the top because they are the one thing on it that
 * can hurt somebody if missed. What the desk can do is decided here, from
 * the visit's state, so the template only shows buttons that will work.
 */
public record AppointmentPanel(
        UUID id,
        String when,
        List<Badge> badges,
        UUID patientId,
        String patientName,
        String initials,
        String avatarClass,
        String patientLine,
        String allergies,
        String doctorLine,
        String bookedFor,
        String bookedOn,
        String checkedIn,
        String attendance,
        String form,
        String waitlist,
        String note,
        String cancelReason,
        boolean cancelled,
        boolean canCheckIn,
        boolean canReschedule,
        boolean canRemind,
        boolean canCancel) {

    public record Badge(String label, String badgeClass) {
    }

    public boolean hasActions() {
        return canCheckIn || canReschedule || canRemind || canCancel;
    }
}
