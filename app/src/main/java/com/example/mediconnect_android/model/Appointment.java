package com.example.mediconnect_android.model;

public class Appointment {
    private int appointmentId;
    private String status;
    private int patientId;
    private Schedule schedule;
    private boolean isVirtual;

    // Getters e Setters
    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
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
        return "Appointment{" +
                "appointmentId=" + appointmentId +
                ", status='" + status + '\'' +
                ", patientId=" + patientId +
                ", schedule=" + schedule +
                ", isVirtual=" + isVirtual +
                '}';
    }

    public Schedule getSchedule() {
        return schedule;
    }
}
