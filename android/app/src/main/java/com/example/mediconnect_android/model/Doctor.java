package com.example.mediconnect_android.model;

public class Doctor {

    private String id;
    private String firstName;
    private String lastName;
    private String name;
    private int clinicId;
    private Double score;
    private Integer reviewCount;
    private String specialty;
    private String photo;
    /** Years in practice, shown beside the specialty. */
    private String experienceYears;
    /**
     * ISO-8601 local date-time of this doctor's next free slot, or null when
     * they have nothing open. A timestamp, not a label: whether it reads
     * "Today 2:30 PM" or "Next: Thu 1 Oct" depends on the day.
     */
    private String nextAvailableAt;

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(String experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getNextAvailableAt() {
        return nextAvailableAt;
    }

    public void setNextAvailableAt(String nextAvailableAt) {
        this.nextAvailableAt = nextAvailableAt;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getClinicId() {
        return clinicId;
    }

    public void setClinicId(int clinicId) {
        this.clinicId = clinicId;
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
