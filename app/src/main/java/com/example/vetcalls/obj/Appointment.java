package com.example.vetcalls.obj;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a veterinary appointment in the VetCalls application.
 * Implements Serializable to allow passing between activities and fragments.
 * Contains all necessary information about appointments including participants, timing, and status.
 *
 * @author Ofek Levi
 */
public class Appointment implements java.io.Serializable {

    /** Unique identifier for the appointment */
    public String id;

    /** Date of the appointment in string format */
    public String date;

    /** Start time of the appointment */
    public String startTime;

    /** End time of the appointment */
    public String endTime;

    /** Type or category of the appointment */
    public String type;

    /** Unique identifier of the dog */
    public String dogId;

    /** Name of the dog */
    public String dogName;

    /** Unique identifier of the veterinarian */
    public String vetId;

    /** Name of the veterinarian */
    public String vetName;

    /** Unique identifier of the dog owner */
    public String ownerId;

    /** Whether the appointment has been completed */
    public boolean isCompleted;

    /** Additional notes or details about the appointment */
    public String notes;

    /** First reminder timestamp */
    public com.google.firebase.Timestamp reminder1;

    /** Second reminder timestamp */
    public com.google.firebase.Timestamp reminder2;

    /**
     * Default constructor required for Firebase serialization.
     */
    public Appointment() {}

    /**
     * Full constructor for creating a complete appointment object.
     *
     * @param id Unique identifier for the appointment
     * @param date Date of the appointment
     * @param startTime Start time of the appointment
     * @param endTime End time of the appointment
     * @param type Type or category of the appointment
     * @param dogId Unique identifier of the dog
     * @param dogName Name of the dog
     * @param vetId Unique identifier of the veterinarian
     * @param vetName Name of the veterinarian
     * @param ownerId Unique identifier of the dog owner
     * @param isCompleted Whether the appointment has been completed
     * @param notes Additional notes or details about the appointment
     * @param reminder1 First reminder timestamp
     * @param reminder2 Second reminder timestamp
     */
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

    /**
     * Simplified constructor for basic appointment creation.
     *
     * @param date Date of the appointment
     * @param time Start time of the appointment
     * @param details Notes or details about the appointment
     * @param veterinarian Name of the veterinarian
     */
    public Appointment(String date, String time, String details, String veterinarian) {
        this.date = date;
        this.startTime = time;
        this.notes = details;
        this.vetName = veterinarian;
    }
}