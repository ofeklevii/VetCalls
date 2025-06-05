package com.example.vetcalls.obj;

/**
 * Model class representing a user in the VetCalls application.
 * Contains basic user information including authentication details and role identification.
 * Used for Firebase Firestore serialization and user management.
 *
 * @author Ofek Levi
 */
public class User {

    /** User's email address */
    public String email;

    /** Whether the user is a veterinarian */
    public Boolean isVet;

    /** Unique identifier for the user */
    public String userId;

    /**
     * Default constructor required for Firebase Firestore serialization.
     */
    public User() {
    }

    /**
     * Constructor for creating a user with all required information.
     *
     * @param email User's email address
     * @param isVet Whether the user is a veterinarian
     * @param userId Unique identifier for the user
     */
    public User(String email, Boolean isVet, String userId) {
        this.email = email;
        this.isVet = isVet;
        this.userId = userId;
    }
}