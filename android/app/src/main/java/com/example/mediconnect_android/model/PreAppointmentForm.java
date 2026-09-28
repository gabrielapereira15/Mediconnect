package com.example.mediconnect_android.model;

public class PreAppointmentForm {
    private String title;
    private String status;
    private PreAppointmentFormDetails formDetails;

    public PreAppointmentForm(String title, String status, PreAppointmentFormDetails formDetails) {
        this.title = title;
        this.status = status;
        this.formDetails = formDetails;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PreAppointmentFormDetails getFormDetails() {
        return formDetails;
    }

    public void setFormDetails(PreAppointmentFormDetails formDetails) {
        this.formDetails = formDetails;
    }
}
