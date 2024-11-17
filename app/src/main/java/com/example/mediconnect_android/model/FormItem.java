package com.example.mediconnect_android.model;

public class FormItem {
    private String title;
    private String status;
    private String description;

    // Constructor
    public FormItem(String title, String status, String description) {
        this.title = title;
        this.status = status;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
