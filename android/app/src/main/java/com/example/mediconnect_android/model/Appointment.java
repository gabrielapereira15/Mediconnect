package com.example.mediconnect_android.model;

public class Appointment {
    private String id;
    private int patientId;
    private String status;
    private String date;
    private String time;
    /** ISO-8601 local date-time from the API; safe to parse, unlike date/time. */
    private String startsAt;
    private Doctor doctor;
    private Schedule schedule;
    private Boolean reviewed;
    /** The score this patient gave, or null if they have not reviewed it. */
    private Float reviewScore;
    private boolean isVirtual;
    /** Who the visit is for, or null when it is for the account holder. */
    private String bookedForName;


    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public String getBookedForName() {
        return bookedForName;
    }

    public void setBookedForName(String bookedForName) {
        this.bookedForName = bookedForName;
    }

    public Float getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(Float reviewScore) {
        this.reviewScore = reviewScore;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Schedule getschedule() {
        return schedule;
    }

    public void setschedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    @Override
    public String toString() {
        return date + " | " + time;
    }

    public String getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(String startsAt) {
        this.startsAt = startsAt;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
