package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.staff.StaffRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Who may use a back-office handler, checked on the server.
 *
 * Hiding the Patients link from the front desk kept nobody out of a
 * patient's chart who could type its address. Put on a controller, it
 * covers every handler in it; on a method, it covers that one and wins over
 * the controller's. Anything unannotated is open to all signed-in staff.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    StaffRole value();
}
