package com.example.mediconnect_android.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Regression tests for setters that used to assign their parameter to itself:
 *
 *     public void setfirstName(String firstName) {
 *         firstName = firstName;   // the field was never written
 *     }
 *
 * Gson populates fields by reflection, so reading from the API still worked and
 * the bug was invisible until a profile edit silently lost the values.
 */
public class PatientTest {

    @Test
    public void settersWriteToTheField() {
        var patient = new Patient();

        patient.setfirstName("Gabriela");
        patient.setlastName("Pereira");
        patient.setemail("demo@mediconnect.ca");
        patient.setphoneNumber("226-899-0000");
        patient.setclinicCode("KIT001");
        patient.setaddress("15 Wellington Street, Kitchener, ON");
        patient.setgender("Female");

        assertEquals("Gabriela", patient.getfirstName());
        assertEquals("Pereira", patient.getlastName());
        assertEquals("demo@mediconnect.ca", patient.getemail());
        assertEquals("226-899-0000", patient.getphoneNumber());
        assertEquals("KIT001", patient.getclinicCode());
        assertEquals("15 Wellington Street, Kitchener, ON", patient.getaddress());
        assertEquals("Female", patient.getgender());
    }

    @Test
    public void appointmentReviewedFlagIsStored() {
        var appointment = new Appointment();

        appointment.setReviewed(true);

        assertEquals(Boolean.TRUE, appointment.getReviewed());
    }
}
