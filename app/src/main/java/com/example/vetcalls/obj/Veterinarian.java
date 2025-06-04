package com.example.vetcalls.obj;

public class Veterinarian {
    public String fullName;
    public String clinicAddress;
    public String workHoursFirstPart;
    public String workHoursSecondPart;
    public String workHoursThirdPart;
    public String profileImageUrl;
    public String email;
    public String phoneNumber;
    public boolean isVet;
    public String uid;

    public Veterinarian() {}

    public Veterinarian(String fullName, String clinicAddress,
                        String workHoursFirstPart, String workHoursSecondPart,
                        String workHoursThirdPart, String profileImageUrl, String email, String phoneNumber, boolean isVet, String uid) {
        this.fullName = fullName;
        this.clinicAddress = clinicAddress;
        this.workHoursFirstPart = workHoursFirstPart;
        this.workHoursSecondPart = workHoursSecondPart;
        this.workHoursThirdPart = workHoursThirdPart;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isVet = isVet;
        this.uid = uid;
    }
} 