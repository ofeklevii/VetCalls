package com.example.vetcalls.obj;

import com.google.firebase.Timestamp;
import java.io.Serializable; // הוספת Serializable
import java.util.HashMap;
import java.util.Map;

public class Appointment implements java.io.Serializable {
    public String id;
    public String date;
    public String startTime;
    public String endTime;
    public String type;
    public String dogId;
    public String dogName;
    public String vetId;
    public String vetName;
    public String ownerId;
    public boolean isCompleted;
    public String notes;
    public com.google.firebase.Timestamp reminder1;
    public com.google.firebase.Timestamp reminder2;

    public Appointment() {}
    public Appointment(String id, String date, String startTime, String endTime, String type,
                       String dogId, String dogName, String vetId, String vetName,
                       String ownerId, boolean isCompleted, String notes,
                       com.google.firebase.Timestamp reminder1, com.google.firebase.Timestamp reminder2) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.dogId = dogId;
        this.dogName = dogName;
        this.vetId = vetId;
        this.vetName = vetName;
        this.ownerId = ownerId;
        this.isCompleted = isCompleted;
        this.notes = notes;
        this.reminder1 = reminder1;
        this.reminder2 = reminder2;
    }
    public Appointment(String date, String time, String details, String veterinarian) {
        this.date = date;
        this.startTime = time;
        this.notes = details;
        this.vetName = veterinarian;
    }
}