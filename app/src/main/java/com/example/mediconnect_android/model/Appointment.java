package com.example.mediconnect_android.model;

import java.util.List;

public class Appointment {
    private String id;
    private int patientId;
    private String status;
    private String date;
    private String time;
    private Doctor doctor;
    private Schedule schedule;
    private Boolean reviewed;
    private boolean isVirtual;


    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        reviewed = reviewed;
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
    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
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
