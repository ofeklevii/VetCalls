package com.example.vetcalls.obj;

public class DogProfile {
    private String name;
    private String age;
    private String bio;
    private String imageUrl;

    public DogProfile() {}

    public DogProfile(String name, String age, String bio, String imageUrl) {
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getAge() { return age; }
    public String getBio() { return bio; }
    public String getImageUrl() { return imageUrl; }
}

