package com.example.vetcalls.obj;

public class Appointment {
    private String date;
    private String time;
    private String details;
    private String veterinarian;

    public Appointment(String date, String time, String details, String veterinarian) {
        this.date = date;
        this.time = time;
        this.details = details;
        this.veterinarian = veterinarian;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getVeterinarian(){
        return veterinarian;
    }

    public String getDetails() {
        return details;
    }
}
