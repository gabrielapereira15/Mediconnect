package com.example.mediconnect_android.model;

public class Form {
    private String title;
    private String status;
    private String description;

    public Form(String title, String status, String description) {
        this.title = title;
        this.status = status;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
