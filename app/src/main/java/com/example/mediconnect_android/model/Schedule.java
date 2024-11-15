package com.example.mediconnect_android.model;

import java.util.List;

public class Schedule {
    private int scheduleId;
    private Doctor doctor;
    private String date;
    private List<String> times;

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List times) {
        this.times = times;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    @Override
    public String toString() {
        return date + " - " + times;
    }

}
