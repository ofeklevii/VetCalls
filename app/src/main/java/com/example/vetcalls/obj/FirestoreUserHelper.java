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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

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
        userData.put("uid", user.getUid());

        if (!isVet && vetIdIfUserHas != null) {
            userData.put("vetId", vetIdIfUserHas);
        }

        // שמירת כל המשתמשים בקולקשיין Users
        db.collection("Users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved to Users collection"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));

        // אם המשתמש הוא וטרינר, נשמור אותו גם בקולקשיין Veterinarians
        if (isVet) {
            db.collection("Veterinarians").document(user.getUid())
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Vet profile saved to Veterinarians collection"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving vet profile", e));
        }
    }

    // הוספת פרופיל כלב - רק לאוסף DogProfiles
    public static void addDogProfile(String dogId, String name, String race, String age,
                                     String birthday, String weight, String allergies,
                                     String vaccines, String bio,
                                     String ownerId, String vetId,
                                     @Nullable String imageUrl) {

        Log.d(TAG, "Adding dog profile: " + dogId + ", name: " + name + ", owner: " + ownerId);

        Map<String, Object> dogData = new HashMap<>();
        dogData.put("name", name);
        dogData.put("race", race);
        dogData.put("age", age); // שימוש ב-String במקום int
        dogData.put("birthday", birthday);
        dogData.put("weight", weight);
        dogData.put("allergies", allergies);
        dogData.put("vaccines", vaccines);
        dogData.put("bio", bio);
        dogData.put("ownerId", ownerId);

        if (vetId != null && !vetId.isEmpty()) {
            dogData.put("vetId", vetId);
        }

        if (imageUrl != null) {
            dogData.put("imageUrl", imageUrl); // imageUrl במקום profileImageUrl
        }

        // שמירה באוסף DogProfiles בלבד
        db.collection("DogProfiles").document(dogId)
                .set(dogData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog profile saved to DogProfiles");
                    // הוספת reference לכלב במסמך של המשתמש
                    updateUserDogReferences(ownerId, dogId, name);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save dog profile", e));
    }

    // עדכון רשימת הכלבים של המשתמש
    private static void updateUserDogReferences(String ownerId, String dogId, String dogName) {
        Map<String, Object> dogRef = new HashMap<>();
        dogRef.put("dogId", dogId);
        dogRef.put("name", dogName);

        // הוספת reference בתת-קולקשיין של המשתמש
        db.collection("Users").document(ownerId)
                .collection("Dogs").document(dogId)
                .set(dogRef)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog reference added to user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add dog reference", e));
    }

    // העלאת תמונת פרופיל של כלב
    public static void uploadDogProfileImage(Uri imageUri, String dogId, String ownerId, OnImageUploadListener listener) {
        if (imageUri == null) {
            if (listener != null) {
                listener.onUploadFailed(new IllegalArgumentException("Image URI is null"));
            }
            return;
        }

        StorageReference storageRef = storage.getReference()
                .child("dog_profile_images/" + dogId + ".jpg");

        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();

                // עדכון ה-URL בקולקשיין DogProfiles בלבד
                db.collection("DogProfiles").document(dogId)
                        .update("imageUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Image URL updated in DogProfiles");
                            if (listener != null) {
                                listener.onUploadSuccess(imageUrl);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update image URL", e);
                            if (listener != null) {
                                listener.onUploadFailed(e);
                            }
                        });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL", e);
                if (listener != null) {
                    listener.onUploadFailed(e);
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image", e);
            if (listener != null) {
                listener.onUploadFailed(e);
            }
        });
    }

    public interface OnImageUploadListener {
        void onUploadSuccess(String imageUrl);
        void onUploadFailed(Exception e);
    }

    public static void addAppointment(String appointmentId, Map<String, Object> appointmentData) {
        String dogId = (String) appointmentData.get("dogId");
        String vetId = (String) appointmentData.get("vetId");

        if (dogId != null && !dogId.isEmpty()) {
            // שמירה באוסף של הכלב
            db.collection("DogProfiles")
                    .document(dogId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .set(appointmentData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to dog"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to dog", e));
        }

        if (vetId != null && !vetId.isEmpty()) {
            // שמירה באוסף של הוטרינר
            db.collection("Veterinarians")
                    .document(vetId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .set(appointmentData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to vet"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to vet", e));
        }
    }

    // מחיקת פגישה
    public static void deleteAppointment(String appointmentId, String dogId, String vetId) {
        if (dogId != null && !dogId.isEmpty()) {
            db.collection("DogProfiles")
                    .document(dogId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment deleted from dog"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete appointment from dog", e));
        }

        if (vetId != null && !vetId.isEmpty()) {
            db.collection("Veterinarians")
                    .document(vetId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment deleted from vet"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete appointment from vet", e));
        }
    }

    public static void addReminderToUser(@NonNull String userId, @NonNull String reminderId, Map<String, Object> reminderData) {
        db.collection("Users").document(userId)
                .collection("Reminders").document(reminderId)
                .set(reminderData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reminder added to user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add reminder to user", e));
    }

    public static void deleteUserCompletely(String userId, Runnable onSuccess, Runnable onFailure) {
        Log.d(TAG, "Starting deletion process for user: " + userId);

        // רשימת משימות מחיקה לעקוב אחריהן
        List<Task<Void>> deleteTasks = new ArrayList<>();

        // 1. קודם כל מחק את הכלבים של המשתמש
        db.collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(dogSnapshots -> {
                    Log.d(TAG, "Found " + dogSnapshots.size() + " dogs to delete");

                    for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                        String dogId = dogDoc.getString("dogId");
                        if (dogId != null && !dogId.isEmpty()) {
                            // מחק את התורים של הכלב
                            deleteDogAppointments(dogId, deleteTasks);

                            // מחק את הכלב עצמו מקולקשיין DogProfiles
                            Task<Void> deleteProfileTask = db.collection("DogProfiles")
                                    .document(dogId)
                                    .delete();
                            deleteTasks.add(deleteProfileTask);

                            // מחק את הכלב מקולקשיין הכלבים של המשתמש
                            Task<Void> deleteDogTask = dogDoc.getReference().delete();
                            deleteTasks.add(deleteDogTask);
                        }
                    }

                    // 2. מחק את התזכורות של המשתמש
                    deleteUserReminders(userId, deleteTasks);

                    // 3. מחק את כל התורים שהמשתמש קבע (במקרה שהמשתמש הוא וטרינר)
                    if (isUserVet(userId)) {
                        deleteVetAppointments(userId, deleteTasks);
                    }

                    // 4. המתן לסיום כל פעולות המחיקה
                    Tasks.whenAllComplete(deleteTasks)
                            .addOnSuccessListener(results -> {
                                // 5. מחק את מסמך המשתמש עצמו
                                db.collection("Users")
                                        .document(userId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User document deleted successfully");

                                            // 6. מחק את המשתמש ממערכת האימות
                                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                            if (user != null && user.getUid().equals(userId)) {
                                                user.delete()
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            Log.d(TAG, "User authentication deleted successfully");
                                                            if (onSuccess != null) onSuccess.run();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Error deleting user authentication", e);
                                                            if (onFailure != null) onFailure.run();
                                                        });
                                            } else {
                                                if (onSuccess != null) onSuccess.run();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error deleting user document", e);
                                            if (onFailure != null) onFailure.run();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error completing delete tasks", e);
                                if (onFailure != null) onFailure.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user's dogs", e);
                    if (onFailure != null) onFailure.run();
                });
    }

    // פונקציות עזר

    private static void deleteDogAppointments(String dogId, List<Task<Void>> deleteTasks) {
        db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .get()
                .addOnSuccessListener(appointments -> {
                    for (QueryDocumentSnapshot appointment : appointments) {
                        String appointmentId = appointment.getId();
                        String vetId = appointment.getString("vetId");

                        // מחק את התור גם אצל הווטרינר
                        if (vetId != null && !vetId.isEmpty()) {
                            Task<Void> deleteVetAppointment = db.collection("Veterinarians")
                                    .document(vetId)
                                    .collection("Appointments")
                                    .document(appointmentId)
                                    .delete();
                            deleteTasks.add(deleteVetAppointment);
                        }

                        // מחק את התור מהכלב
                        Task<Void> deleteAppointment = appointment.getReference().delete();
                        deleteTasks.add(deleteAppointment);
                    }
                });
    }

    private static void deleteUserReminders(String userId, List<Task<Void>> deleteTasks) {
        db.collection("Users")
                .document(userId)
                .collection("Reminders")
                .get()
                .addOnSuccessListener(reminders -> {
                    for (QueryDocumentSnapshot reminder : reminders) {
                        Task<Void> deleteReminder = reminder.getReference().delete();
                        deleteTasks.add(deleteReminder);
                    }
                });
    }

    private static void deleteVetAppointments(String vetId, List<Task<Void>> deleteTasks) {
        db.collection("Veterinarians")
                .document(vetId)
                .collection("Appointments")
                .get()
                .addOnSuccessListener(appointments -> {
                    for (QueryDocumentSnapshot appointment : appointments) {
                        String appointmentId = appointment.getId();
                        String dogId = appointment.getString("dogId");

                        // מחק את התור גם אצל הכלב
                        if (dogId != null && !dogId.isEmpty()) {
                            Task<Void> deleteDogAppointment = db.collection("DogProfiles")
                                    .document(dogId)
                                    .collection("Appointments")
                                    .document(appointmentId)
                                    .delete();
                            deleteTasks.add(deleteDogAppointment);
                        }

                        // מחק את התור אצל הווטרינר
                        Task<Void> deleteAppointment = appointment.getReference().delete();
                        deleteTasks.add(deleteAppointment);
                    }
                });
    }

    private static boolean isUserVet(String userId) {
        // אתה יכול לממש את זה בהתאם למבנה הנתונים שלך
        // כרגע זו פונקציית דמה שמחזירה true/false
        return true; // שנה את זה בהתאם לבדיקה שלך
    }
}