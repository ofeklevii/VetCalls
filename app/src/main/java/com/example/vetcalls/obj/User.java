package com.example.vetcalls.obj;

public class User {
    public String email;
    public Boolean isVet;
    public String userId;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String email, Boolean isVet, String userId) {
        this.email = email;
        this.isVet = isVet;
        this.userId = userId;
    }
}
