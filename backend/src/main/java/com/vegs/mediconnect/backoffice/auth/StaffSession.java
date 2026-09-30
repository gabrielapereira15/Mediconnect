package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.datasource.staff.StaffUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Who is signed in to the back office, for the length of one session.
 *
 * A server-rendered tool with a session is the right shape here; the token
 * the phone carries exists because a phone has no session to keep.
 */
public final class StaffSession {

    public static final String ATTRIBUTE = "staffUser";

    private final String name;
    private final String email;
    private final StaffRole role;
    private final String clinicCode;
    private final String initials;

    private StaffSession(StaffUser user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.clinicCode = user.getClinicCode();
        this.initials = user.getInitials();
    }

    /** Starts a session, replacing any id an attacker may have planted. */
    public static StaffSession begin(HttpServletRequest request, StaffUser user) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        StaffSession session = new StaffSession(user);
        request.getSession(true).setAttribute(ATTRIBUTE, session);
        return session;
    }

    public static StaffSession of(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ATTRIBUTE);
        return value instanceof StaffSession ? (StaffSession) value : null;
    }

    public static void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public StaffRole getRole() {
        return role;
    }

    public String getRoleLabel() {
        return role == null ? "" : role.getLabel();
    }

    public String getClinicCode() {
        return clinicCode;
    }

    public String getInitials() {
        return initials;
    }

    /** A patient's record is a clinician's to read, not the front desk's. */
    public boolean canReadCharts() {
        return role == StaffRole.CLINICIAN;
    }

    /** Booking, checking in and cancelling belong to the desk. */
    public boolean canManageAppointments() {
        return role == StaffRole.FRONT_DESK;
    }
}
