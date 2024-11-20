package com.example.mediconnect_android.model;
public class Doctor {

    private String id;
    private String firstName;
    private String lastName;
    private int clinicId;
    private String specialty;
    private String photo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getClinicId() {
        return clinicId;
    }

    public void setClinicId(int clinicId) {
        this.clinicId = clinicId;
    }

    public String getName() {
        if (firstName == null && lastName == null) {
            return "Unknown Doctor";
        } else if (lastName == null) {
            return firstName;
        } else if (firstName == null) {
            return lastName;
        }
        return lastName + ", " + firstName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
