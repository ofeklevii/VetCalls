package com.example.vetcalls.obj;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class FirestoreUserHelper {

    private static final String TAG = "FirestoreUserHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    // יצירת משתמש או וטרינר
    public static void createUser(@NonNull FirebaseUser user, boolean isVet, @Nullable String vetIdIfUserHas) {
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("isVet", isVet);
        if (!isVet && vetIdIfUserHas != null) {
            userData.put("vetId", vetIdIfUserHas);
        }

        db.collection("Users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved."))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));

        if (isVet) {
            db.collection("Veterinarians").document(user.getUid())
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Vet profile saved."))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving vet profile", e));
        }
    }

    // שמירה של פרופיל כלב כולל URL של התמונה
    public static void addDogProfile(String dogId, String name, String race, int age,
                                     String birthday, String weight, String allergies,
                                     String vaccines, String bio,
                                     String ownerId, String vetId,
                                     @Nullable String imageUrl) {

        Log.d(TAG, "Adding dog profile: " + dogId + ", name: " + name + ", owner: " + ownerId);

        Map<String, Object> dogData = new HashMap<>();
        dogData.put("name", name);
        dogData.put("race", race);
        dogData.put("age", age);
        dogData.put("birthday", birthday);
        dogData.put("weight", weight);
        dogData.put("allergies", allergies);
        dogData.put("vaccines", vaccines);
        dogData.put("bio", bio);
        dogData.put("ownerId", ownerId);
        if (vetId != null && !vetId.isEmpty()) {
            dogData.put("vetId", vetId);
        }

        // שימוש בשדה עקבי לתמונה - profileImageUrl
        if (imageUrl != null) {
            dogData.put("profileImageUrl", imageUrl);
        }

        // שמירה באוסף הכללי של כלבים - DogProfiles
        db.collection("DogProfiles").document(dogId)
                .set(dogData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog profile saved to DogProfiles"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save dog profile to DogProfiles", e));

        // שמירה בתת-אוסף של המשתמש - Users/{ownerId}/Dogs
        db.collection("Users").document(ownerId)
                .collection("Dogs").document(dogId)
                .set(dogData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog profile saved under user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save dog profile under user", e));

        // בדיקה נוספת כדי לוודא שהנתיב מלא
        if (ownerId != null && !ownerId.isEmpty()) {
            // עדכון רשימת הכלבים של המשתמש
            Map<String, Object> dogRef = new HashMap<>();
            dogRef.put("dogId", dogId);
            dogRef.put("name", name);

            db.collection("Users").document(ownerId)
                    .collection("DogsList").document(dogId)
                    .set(dogRef)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Added dog to user's dogs list"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add dog to user's dogs list", e));
        }
    }

    public static void uploadDogProfileImage(Uri imageUri, String dogId, String ownerId, OnImageUploadListener listener) {
        if (imageUri == null) {
            if (listener != null) {
                listener.onUploadFailed(new IllegalArgumentException("Image URI is null"));
            }
            return;
        }

        StorageReference storageRef = storage.getReference().child("dog_profile_images/" + dogId + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();

                // עדכון ה-URL של התמונה במסמך הכלב
                db.collection("DogProfiles").document(dogId)
                        .update("profileImageUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Image URL updated in DogProfiles"));

                // עדכון ה-URL של התמונה במסמך הכלב תחת המשתמש
                if (ownerId != null && !ownerId.isEmpty()) {
                    db.collection("Users").document(ownerId)
                            .collection("Dogs").document(dogId)
                            .update("profileImageUrl", imageUrl)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Image URL updated in user's Dogs"));
                }

                Log.d(TAG, "Image uploaded. URL: " + imageUrl);
                if (listener != null) {
                    listener.onUploadSuccess(imageUrl);
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                if (listener != null) {
                    listener.onUploadFailed(e);
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image: " + e.getMessage());
            if (listener != null) {
                listener.onUploadFailed(e);
            }
        });
    }


    public interface OnImageUploadListener {
        void onUploadSuccess(String imageUrl);
        void onUploadFailed(Exception e);
    }

    // פגישות ותזכורות
    public static void addAppointmentToDog(@NonNull String dogId, @NonNull String appointmentId, Map<String, Object> appointmentData) {
        db.collection("DogProfiles").document(dogId)
                .collection("Appointments").document(appointmentId)
                .set(appointmentData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to dog"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to dog", e));
    }

    public static void addAppointmentToVetCalendar(@NonNull String vetId, @NonNull String appointmentId, Map<String, Object> appointmentData) {
        db.collection("Veterinarians").document(vetId)
                .collection("Calendar").document(appointmentId)
                .set(appointmentData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to vet calendar"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to vet calendar", e));
    }

    public static void addReminderToUser(@NonNull String userId, @NonNull String reminderId, Map<String, Object> reminderData) {
        db.collection("Users").document(userId)
                .collection("Reminders").document(reminderId)
                .set(reminderData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reminder added to user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add reminder to user", e));
    }
}