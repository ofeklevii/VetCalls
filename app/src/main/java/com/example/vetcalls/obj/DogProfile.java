package com.example.vetcalls.obj;

/**
 * Model class representing a dog profile in the VetCalls application.
 * Contains comprehensive information about a dog including personal details,
 * medical information, and associations with owners and veterinarians.
 *
 * @author Ofek Levi
 */
public class DogProfile {

    /** Unique identifier for the dog */
    public String dogId;

    /** Name of the dog */
    public String name;

    /** Age of the dog stored as String but can be Long in Firestore */
    public String age;

    /** Biography or description of the dog */
    public String bio;

    /** URL for the dog's profile image */
    public String profileImageUrl;

    /** Breed of the dog */
    public String race;

    /** Birthday of the dog */
    public String birthday;

    /** Weight of the dog */
    public String weight;

    /** Known allergies of the dog */
    public String allergies;

    /** Vaccination information */
    public String vaccines;

    /** Unique identifier of the dog's owner */
    public String ownerId;

    /** Unique identifier of the assigned veterinarian */
    public String vetId;

    /** Name of the assigned veterinarian */
    public String vetName;

    /** Timestamp of the last veterinarian change */
    public long lastVetChange;

    /** Timestamp of the last profile update */
    public long lastUpdated;

    /** Whether this is the current active profile */
    public boolean isCurrent;

    /**
     * Default constructor that initializes the profile with default values.
     * Sets the last updated timestamp to current time and age to "Unknown".
     */
    public DogProfile() {
        this.lastUpdated = System.currentTimeMillis();
        this.age = "Unknown";
    }

    /**
     * Returns a string representation of the DogProfile object.
     *
     * @return String containing the dog's basic information
     */
    @Override
    public String toString() {
        return "DogProfile{" +
                "dogId='" + dogId + '\'' +
                ", name='" + name + '\'' +
                ", race='" + race + '\'' +
                ", age='" + age + '\'' +
                '}';
    }

    /**
     * Compares this DogProfile with another object for equality.
     * Two profiles are considered equal if they have the same dogId.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DogProfile that = (DogProfile) obj;
        return dogId != null && dogId.equals(that.dogId);
    }

    /**
     * Returns a hash code value for the object based on the dogId.
     *
     * @return Hash code value for this DogProfile
     */
    @Override
    public int hashCode() {
        return dogId != null ? dogId.hashCode() : 0;
    }

    /**
     * Gets the unique identifier of the dog.
     *
     * @return The dog's unique identifier
     */
    public String getId() {
        return dogId;
    }

    /**
     * Sets whether this profile is the current active profile.
     *
     * @param current true if this is the current profile, false otherwise
     */
    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }
}