package com.example.mediconnect_android.model;

public class CheckinForm {
    private String title;
    private String status;
    private CheckinFormDetails formDetails;

    public CheckinForm(String title, String status, CheckinFormDetails formDetails) {
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

    public CheckinFormDetails getFormDetails() {
        return formDetails;
    }

    public void setFormDetails(CheckinFormDetails formDetails) {
        this.formDetails = formDetails;
    }
}
