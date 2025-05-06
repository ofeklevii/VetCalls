package com.example.vetcalls.obj;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VetAssignmentHelper {

    public static void assignRandomVetToDog(
            String dogId,
            String name,
            String race,
            int age,
            String birthday,
            String weight,
            String allergies,
            String vaccines,
            String bioText,
            String ownerId
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // שליפת וטרינרים
        db.collection("Veterinarians").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        List<DocumentSnapshot> vetList = querySnapshot.getDocuments();
                        Random random = new Random();
                        DocumentSnapshot randomVet = vetList.get(random.nextInt(vetList.size()));
                        String assignedVetId = randomVet.getId();

                        Log.d("VetAssignmentHelper", "Assigned vetId: " + assignedVetId);

                        Map<String, Object> dogData = new HashMap<>();
                        dogData.put("dogId", dogId);
                        dogData.put("name", name);
                        dogData.put("race", race);
                        dogData.put("age", age);
                        dogData.put("birthday", birthday);
                        dogData.put("weight", weight);
                        dogData.put("allergies", allergies);
                        dogData.put("vaccines", vaccines);
                        dogData.put("bio", bioText);
                        dogData.put("ownerId", ownerId);
                        dogData.put("vetId", assignedVetId);

                        // בדוק אם הכלב כבר קיים ב-DogProfiles
                        DocumentReference dogRef = db.collection("DogProfiles").document(dogId);
                        dogRef.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Log.d("VetAssignmentHelper", "Dog profile exists, updating...");

                                updateDogProfile(db, dogId, ownerId, dogData);

                            } else {
                                Log.d("VetAssignmentHelper", "Dog profile does not exist, creating...");

                                createDogProfile(db, dogId, ownerId, dogData);
                            }
                        });
                    } else {
                        Log.w("VetAssignmentHelper", "No veterinarians found to assign.");
                    }
                })
                .addOnFailureListener(e -> Log.e("VetAssignmentHelper", "Failed to assign vet: " + e.getMessage()));
    }

    private static void createDogProfile(FirebaseFirestore db, String dogId, String ownerId, Map<String, Object> dogData) {
        db.collection("DogProfiles").document(dogId).set(dogData)
                .addOnSuccessListener(aVoid -> Log.d("VetAssignmentHelper", "Dog profile created in DogProfiles."))
                .addOnFailureListener(e -> Log.e("VetAssignmentHelper", "Failed to create in DogProfiles: " + e.getMessage()));

        db.collection("Users").document(ownerId)
                .collection("DogProfiles").document(dogId).set(dogData)
                .addOnSuccessListener(aVoid -> Log.d("VetAssignmentHelper", "Dog profile created under User's DogProfiles."))
                .addOnFailureListener(e -> Log.e("VetAssignmentHelper", "Failed to create under User's DogProfiles: " + e.getMessage()));
    }

    private static void updateDogProfile(FirebaseFirestore db, String dogId, String ownerId, Map<String, Object> dogData) {
        db.collection("DogProfiles").document(dogId).update(dogData)
                .addOnSuccessListener(aVoid -> Log.d("VetAssignmentHelper", "Dog profile updated in DogProfiles."))
                .addOnFailureListener(e -> Log.e("VetAssignmentHelper", "Failed to update in DogProfiles: " + e.getMessage()));

        db.collection("Users").document(ownerId)
                .collection("DogProfiles").document(dogId).update(dogData)
                .addOnSuccessListener(aVoid -> Log.d("VetAssignmentHelper", "Dog profile updated under User's DogProfiles."))
                .addOnFailureListener(e -> Log.e("VetAssignmentHelper", "Failed to update under User's DogProfiles: " + e.getMessage()));
    }
}
