package com.example.vetcall.obj;

public class Appointment {
    private String date;
    private String time;
    private String details;
    private String doctor;

    public Appointment(String date, String time, String details, String doctor) {
        this.date = date;
        this.time = time;
        this.details = details;
        this.doctor = doctor;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getDoctor(){
        return doctor;
    }

    public String getDetails() {
        return details;
    }
}
