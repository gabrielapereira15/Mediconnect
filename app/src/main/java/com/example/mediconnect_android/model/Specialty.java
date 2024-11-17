package com.example.mediconnect_android.model;

public class Specialty {
    private String name;
    private int imageResId;

    public Specialty(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
