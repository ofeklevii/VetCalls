package com.example.vetcalls.obj;

/**
 * מחלקה המייצגת פרופיל של כלב באפליקציה
 */
public class DogProfile {
    public String dogId;
    public String name;
    public String age;
    public String bio;
    public String profileImageUrl;
    public String race;
    public String birthday;
    public String weight;
    public String allergies;
    public String vaccines;
    public String ownerId;
    public String vetId;
    public String vetName;
    public long lastVetChange;
    public long lastUpdated;
    public boolean isCurrent;

    public DogProfile() {
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "DogProfile{" +
                "dogId='" + dogId + '\'' +
                ", name='" + name + '\'' +
                ", race='" + race + '\'' +
                ", age='" + age + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DogProfile that = (DogProfile) obj;
        return dogId != null && dogId.equals(that.dogId);
    }

    @Override
    public int hashCode() {
        return dogId != null ? dogId.hashCode() : 0;
    }

    public String getId() {
        return dogId;
    }

    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }
}