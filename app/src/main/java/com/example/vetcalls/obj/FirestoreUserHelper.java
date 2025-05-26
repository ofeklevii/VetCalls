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

        User userObj = new User(user.getEmail(), isVet, user.getUid());

        // שמירת כל המשתמשים בקולקשיין Users
        db.collection("Users").document(user.getUid())
                .set(userObj)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved to Users collection"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));

        // אם המשתמש הוא וטרינר, נשמור אותו גם בקולקשיין Veterinarians
        if (isVet) {
            Veterinarian vet = new Veterinarian();
            vet.email = user.getEmail();
            vet.fullName = ""; // אפשר לעדכן בהמשך
            db.collection("Veterinarians").document(user.getUid())
                    .set(vet)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Vet profile saved to Veterinarians collection"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving vet profile", e));
        }
    }

    // העלאת תמונת פרופיל של וטרינר
    public static void uploadVetProfileImage(Uri imageUri, String vetId, OnImageUploadListener listener) {
        if (imageUri == null) {
            if (listener != null) {
                listener.onUploadFailed(new IllegalArgumentException("Image URI is null"));
            }
            return;
        }

        Log.d(TAG, "Starting upload of vet profile image for vet ID: " + vetId);

        StorageReference storageRef = storage.getReference()
                .child("vet_profile_images/" + vetId + ".jpg");

        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Log.d(TAG, "Vet image upload successful, getting download URL");
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d(TAG, "Vet image URL: " + imageUrl);

                // עדכון ה-URL בקולקשיין Veterinarians
                Veterinarian vet = new Veterinarian();
                vet.profileImageUrl = imageUrl;

                db.collection("Veterinarians").document(vetId)
                        .set(vet, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Vet image URL updated in Veterinarians collection");
                            if (listener != null) {
                                listener.onUploadSuccess(imageUrl);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update vet image URL", e);
                            if (listener != null) {
                                listener.onUploadFailed(e);
                            }
                        });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get vet image download URL", e);
                if (listener != null) {
                    listener.onUploadFailed(e);
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload vet image", e);
            if (listener != null) {
                listener.onUploadFailed(e);
            }
        });
    }

    // הוספת פרופיל כלב - רק לאוסף DogProfiles
    public static void addDogProfile(String dogId, String name, String race, String age, String birthday,
                                     String weight, String allergies, String vaccines, String bio,
                                     String ownerId, String vetId, String vetName, long lastVetChange) {
        Log.d(TAG, "Adding dog profile: " + dogId + ", name: " + name + ", owner: " + ownerId);

        DogProfile dogProfile = new DogProfile();
        dogProfile.dogId = dogId;
        dogProfile.name = name;
        dogProfile.race = race;
        dogProfile.age = age;
        dogProfile.birthday = birthday;
        dogProfile.weight = weight;
        dogProfile.allergies = allergies;
        dogProfile.vaccines = vaccines;
        dogProfile.bio = bio;
        dogProfile.ownerId = ownerId;
        dogProfile.vetId = vetId;
        dogProfile.vetName = vetName;
        dogProfile.lastVetChange = lastVetChange;
        dogProfile.lastUpdated = System.currentTimeMillis();

        // Save to DogProfiles collection
        db.collection("DogProfiles").document(dogId)
                .set(dogProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog profile saved to DogProfiles");
                    // Add reference to user's dogs collection
                    updateUserDogReferences(ownerId, dogId, name);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save dog profile", e));
    }

    // עדכון רשימת הכלבים של המשתמש
    private static void updateUserDogReferences(String ownerId, String dogId, String dogName) {
        DogProfile dogRef = new DogProfile();
        dogRef.dogId = dogId;
        dogRef.name = dogName;
        dogRef.lastUpdated = System.currentTimeMillis();

        // Add reference to user's dogs subcollection
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
                DogProfile dogProfile = new DogProfile();
                dogProfile.profileImageUrl = imageUrl;

                db.collection("DogProfiles").document(dogId)
                        .set(dogProfile, SetOptions.merge())
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

    // הוספת פונקציה חדשה למחיקה מלאה של תור עם callback-ים
    public static void deleteAppointmentCompletely(String appointmentId, String dogId, String vetId,
                                                   Runnable onSuccess,
                                                   java.util.function.Consumer<String> onFailure) {
        Log.d(TAG, "Starting complete deletion of appointment: " + appointmentId);

        // מחיקה במקביל משני המקומות
        Task<Void> deleteDogAppointment = db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .document(appointmentId)
                .delete();

        Task<Void> deleteVetAppointment = db.collection("Veterinarians")
                .document(vetId)
                .collection("Appointments")
                .document(appointmentId)
                .delete();

        // חכה שהשתיים יסתיימו
        Tasks.whenAllComplete(deleteDogAppointment, deleteVetAppointment)
                .addOnCompleteListener(task -> {
                    boolean allSuccessful = true;
                    StringBuilder errorMessages = new StringBuilder();

                    for (Task<?> individualTask : task.getResult()) {
                        if (!individualTask.isSuccessful()) {
                            allSuccessful = false;
                            Exception exception = individualTask.getException();
                            if (exception != null) {
                                errorMessages.append(exception.getMessage()).append("; ");
                            }
                        }
                    }

                    if (allSuccessful) {
                        Log.d(TAG, "Appointment deleted completely from all locations");
                        onSuccess.run();
                    } else {
                        Log.e(TAG, "Failed to delete appointment from some locations: " + errorMessages.toString());
                        onFailure.accept("Failed to delete from some locations: " + errorMessages.toString());
                    }
                });
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

        // שלב 1: בדיקה האם המשתמש קיים
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Log.e(TAG, "User document not found: " + userId);
                        if (onFailure != null) onFailure.run();
                        return;
                    }

                    // מעקב אחר משימות מחיקה
                    List<Task<Void>> deleteTasks = new ArrayList<>();

                    // שלב 2: מחיקת תיקיות התמונות בסטורג' - תחילה נאסוף את מזהי הכלבים
                    db.collection("Users").document(userId).collection("Dogs").get()
                            .addOnSuccessListener(dogSnapshots -> {
                                Log.d(TAG, "Found " + dogSnapshots.size() + " dogs to delete");

                                // רשימת מזהי הכלבים לצורך מחיקה מאוחר יותר
                                List<String> dogIds = new ArrayList<>();

                                // רשימת משימות מחיקת תמונות מהסטורג'
                                List<Task<Void>> deleteImageTasks = new ArrayList<>();

                                for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                                    String dogId = dogDoc.getString("dogId");
                                    if (dogId != null && !dogId.isEmpty()) {
                                        dogIds.add(dogId);

                                        // מחיקת תמונת הפרופיל של הכלב מהסטורג'
                                        StorageReference imageRef = storage.getReference()
                                                .child("dog_profile_images/" + dogId + ".jpg");
                                        Task<Void> deleteImageTask = imageRef.delete();
                                        deleteImageTasks.add(deleteImageTask);
                                    }
                                }

                                // בדיקה אם המשתמש הוא וטרינר - במקרה כזה נמחק גם את תמונת הפרופיל שלו
                                Boolean isVet = userDoc.getBoolean("isVet");
                                if (isVet != null && isVet) {
                                    // מחיקת תמונת הפרופיל של הוטרינר מהסטורג'
                                    StorageReference vetImageRef = storage.getReference()
                                            .child("vet_profile_images/" + userId + ".jpg");
                                    Task<Void> deleteVetImageTask = vetImageRef.delete();
                                    deleteImageTasks.add(deleteVetImageTask);
                                }

                                // שלב 3: המתן לסיום מחיקת התמונות ואז התחל בתהליך מחיקת הנתונים
                                Tasks.whenAllComplete(deleteImageTasks)
                                        .addOnCompleteListener(task -> {
                                            deleteUserData(userId, dogIds, onSuccess, onFailure);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching user's dogs", e);
                                if (onFailure != null) onFailure.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user document", e);
                    if (onFailure != null) onFailure.run();
                });
    }

    // פונקציה נפרדת למחיקת נתוני המשתמש לאחר מחיקת התמונות
    private static void deleteUserData(String userId, List<String> dogIds, Runnable onSuccess, Runnable onFailure) {
        Log.d(TAG, "Starting data deletion for user: " + userId + " with " + dogIds.size() + " dogs");

        // רשימת משימות מחיקה
        List<Task<Void>> deleteTasks = new ArrayList<>();

        // שלב 1: מחיקת כל הכלבים מקולקשיין DogProfiles
        for (String dogId : dogIds) {
            // מחיקת התורים של הכלב
            db.collection("DogProfiles").document(dogId).collection("Appointments").get()
                    .addOnSuccessListener(appointments -> {
                        for (QueryDocumentSnapshot appointment : appointments) {
                            String appointmentId = appointment.getId();
                            String vetId = appointment.getString("vetId");

                            // מחיקת התור מהכלב
                            Task<Void> deleteAppointmentTask = appointment.getReference().delete();
                            deleteTasks.add(deleteAppointmentTask);

                            // מחיקת התור גם אצל הווטרינר אם קיים
                            if (vetId != null && !vetId.isEmpty()) {
                                Task<Void> deleteVetAppointment = db.collection("Veterinarians")
                                        .document(vetId)
                                        .collection("Appointments")
                                        .document(appointmentId)
                                        .delete();
                                deleteTasks.add(deleteVetAppointment);
                            }
                        }
                    });

            // מחיקת הכלב עצמו מ-DogProfiles
            Task<Void> deleteDogTask = db.collection("DogProfiles").document(dogId).delete();
            deleteTasks.add(deleteDogTask);
            Log.d(TAG, "Added task to delete dog: " + dogId);
        }

        // שלב 2: מחיקת התזכורות של המשתמש
        db.collection("Users").document(userId).collection("Reminders").get()
                .addOnSuccessListener(reminders -> {
                    for (QueryDocumentSnapshot reminder : reminders) {
                        Task<Void> deleteReminderTask = reminder.getReference().delete();
                        deleteTasks.add(deleteReminderTask);
                    }
                });

        // שלב 3: האם המשתמש הוא וטרינר? אם כן, מחק גם את התורים שלו
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean isVet = userDoc.getBoolean("isVet");
                    if (isVet != null && isVet) {
                        db.collection("Veterinarians").document(userId).collection("Appointments").get()
                                .addOnSuccessListener(appointments -> {
                                    for (QueryDocumentSnapshot appointment : appointments) {
                                        String appointmentId = appointment.getId();
                                        String dogId = appointment.getString("dogId");

                                        // מחיקת התור אצל הווטרינר
                                        Task<Void> deleteAppointmentTask = appointment.getReference().delete();
                                        deleteTasks.add(deleteAppointmentTask);

                                        // מחיקת התור גם אצל הכלב אם קיים
                                        if (dogId != null && !dogId.isEmpty()) {
                                            Task<Void> deleteDogAppointment = db.collection("DogProfiles")
                                                    .document(dogId)
                                                    .collection("Appointments")
                                                    .document(appointmentId)
                                                    .delete();
                                            deleteTasks.add(deleteDogAppointment);
                                        }
                                    }
                                });

                        // מחיקת הווטרינר מקולקשיין Veterinarians
                        Task<Void> deleteVetTask = db.collection("Veterinarians").document(userId).delete();
                        deleteTasks.add(deleteVetTask);
                    }

                    // שלב 4: מחיקת תת-הקולקשיין Dogs של המשתמש
                    db.collection("Users").document(userId).collection("Dogs").get()
                            .addOnSuccessListener(dogsCollection -> {
                                for (QueryDocumentSnapshot dogDoc : dogsCollection) {
                                    Task<Void> deleteDogRefTask = dogDoc.getReference().delete();
                                    deleteTasks.add(deleteDogRefTask);
                                }

                                // שלב 5: המתן לסיום כל משימות המחיקה ואז מחק את המשתמש עצמו
                                Tasks.whenAllComplete(deleteTasks)
                                        .addOnSuccessListener(results -> {
                                            Log.d(TAG, "All delete tasks completed. Results: " + results.size());

                                            // ספירת השגיאות
                                            int errorCount = 0;
                                            for (Task task : results) {
                                                if (!task.isSuccessful()) {
                                                    errorCount++;
                                                }
                                            }
                                            Log.d(TAG, "Tasks completed with " + errorCount + " errors");

                                            // מחיקת מסמך המשתמש עצמו
                                            db.collection("Users").document(userId).delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "User document deleted successfully");

                                                        // מחיקת המשתמש ממערכת האימות
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
                                            Log.e(TAG, "Error waiting for delete tasks", e);
                                            if (onFailure != null) onFailure.run();
                                        });
                            });
                });
    }

    /**
     * עדכון גלובלי של פרופיל כלב בכל המקומות הרלוונטיים במערכת
     */
    public static void updateDogProfileEverywhere(DogProfile dogProfile) {
        if (dogProfile == null || dogProfile.dogId == null) return;
        String dogId = dogProfile.dogId;
        String ownerId = dogProfile.ownerId;
        String vetId = dogProfile.vetId;
        String name = dogProfile.name;
        String imageUrl = dogProfile.profileImageUrl;
        // 1. עדכון ב-DogProfiles
        db.collection("DogProfiles").document(dogId)
                .set(dogProfile, SetOptions.merge());
        // 2. עדכון בתת-קולקשיין Dogs של המשתמש
        if (ownerId != null) {
            db.collection("Users").document(ownerId)
                    .collection("Dogs").document(dogId)
                    .set(dogProfile, SetOptions.merge());
        }
        // 3. עדכון בתת-קולקשיין Patients של הווטרינר
        if (vetId != null) {
            db.collection("Veterinarians").document(vetId)
                    .collection("Patients").document(dogId)
                    .set(dogProfile, SetOptions.merge());
        }
        // 4. עדכון בצ'אטים (שם ותמונה)
        db.collection("Chats")
                .whereEqualTo("dogId", dogId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot chatDoc : querySnapshot.getDocuments()) {
                        chatDoc.getReference().update(
                                "dogName", name,
                                "dogImageUrl", imageUrl != null ? imageUrl : ""
                        );
                    }
                });
    }
}