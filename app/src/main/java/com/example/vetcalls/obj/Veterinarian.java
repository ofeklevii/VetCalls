package com.example.vetcalls.obj;

/**
 * Model class representing a veterinarian in the VetCalls application.
 * Contains comprehensive veterinarian information including personal details,
 * clinic information, work hours, and contact information.
 *
 * @author Ofek Levi
 */
public class Veterinarian {

    /** Veterinarian's full name */
    public String fullName;

    /** Address of the veterinarian's clinic */
    public String clinicAddress;

    /** First part of work hours schedule */
    public String workHoursFirstPart;

    /** Second part of work hours schedule */
    public String workHoursSecondPart;

    /** Third part of work hours schedule */
    public String workHoursThirdPart;

    /** URL for the veterinarian's profile image */
    public String profileImageUrl;

    /** Veterinarian's email address */
    public String email;

    /** Veterinarian's phone number */
    public String phoneNumber;

    /** Whether this user is a veterinarian */
    public boolean isVet;

    /** Unique identifier for the veterinarian */
    public String uid;

    /**
     * Default constructor required for Firebase Firestore serialization.
     */
    public Veterinarian() {}

    /**
     * Constructor for creating a veterinarian with all required information.
     *
     * @param fullName Veterinarian's full name
     * @param clinicAddress Address of the veterinarian's clinic
     * @param workHoursFirstPart First part of work hours schedule
     * @param workHoursSecondPart Second part of work hours schedule
     * @param workHoursThirdPart Third part of work hours schedule
     * @param profileImageUrl URL for the veterinarian's profile image
     * @param email Veterinarian's email address
     * @param phoneNumber Veterinarian's phone number
     * @param isVet Whether this user is a veterinarian
     * @param uid Unique identifier for the veterinarian
     */
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