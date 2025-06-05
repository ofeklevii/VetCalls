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

/**
 * Helper class for managing user and veterinarian data operations in Firestore.
 * Provides comprehensive functionality for user creation, profile management,
 * image uploads, appointment handling, and data synchronization across collections.
 *
 * @author Ofek Levi
 */
public class FirestoreUserHelper {

    private static final String TAG = "FirestoreUserHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Creates a new user or veterinarian in the Firestore database.
     * Saves user data to the Users collection and additionally to Veterinarians collection if the user is a vet.
     *
     * @param user The authenticated Firebase user
     * @param isVet Whether the user is a veterinarian
     * @param vetIdIfUserHas Optional veterinarian ID if the user has one assigned
     */
    public static void createUser(@NonNull FirebaseUser user, boolean isVet, @Nullable String vetIdIfUserHas) {
        if (user == null) return;

        User userObj = new User(user.getEmail(), isVet, user.getUid());

        db.collection("Users").document(user.getUid())
                .set(userObj)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved to Users collection"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));

        if (isVet) {
            Veterinarian vet = new Veterinarian();
            vet.email = user.getEmail();
            vet.fullName = "";
            db.collection("Veterinarians").document(user.getUid())
                    .set(vet)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Vet profile saved to Veterinarians collection"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving vet profile", e));
        }
    }

    /**
     * Uploads a veterinarian's profile image to Firebase Storage and updates the database.
     *
     * @param imageUri The URI of the image to upload
     * @param vetId The unique identifier of the veterinarian
     * @param listener Callback listener for upload success or failure
     */
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

    /**
     * Adds a new dog profile to the DogProfiles collection and updates user references.
     *
     * @param dogId Unique identifier for the dog
     * @param name Dog's name
     * @param race Dog's breed
     * @param age Dog's age
     * @param birthday Dog's birthday
     * @param weight Dog's weight
     * @param allergies Dog's allergies information
     * @param vaccines Dog's vaccination information
     * @param bio Dog's biography
     * @param ownerId Owner's unique identifier
     * @param vetId Assigned veterinarian's unique identifier
     * @param vetName Assigned veterinarian's name
     * @param lastVetChange Timestamp of last veterinarian change
     */
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

        db.collection("DogProfiles").document(dogId)
                .set(dogProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog profile saved to DogProfiles");
                    updateUserDogReferences(ownerId, dogId, name);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save dog profile", e));
    }

    /**
     * Updates the user's dog references by copying the full dog profile to the user's Dogs subcollection.
     *
     * @param ownerId The owner's unique identifier
     * @param dogId The dog's unique identifier
     * @param dogName The dog's name
     */
    private static void updateUserDogReferences(String ownerId, String dogId, String dogName) {
        db.collection("DogProfiles").document(dogId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                DogProfile dog = doc.toObject(DogProfile.class);
                if (dog != null) {
                    db.collection("Users").document(ownerId)
                            .collection("Dogs").document(dogId)
                            .set(dog)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog reference added to user with all fields"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to add dog reference", e));
                }
            }
        });
    }

    /**
     * Uploads a dog's profile image to Firebase Storage and updates the database.
     * Also triggers global updates across all relevant collections.
     *
     * @param imageUri The URI of the image to upload
     * @param dogId The dog's unique identifier
     * @param ownerId The owner's unique identifier
     * @param listener Callback listener for upload success or failure
     */
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

                DogProfile dogProfile = new DogProfile();
                dogProfile.profileImageUrl = imageUrl;

                db.collection("DogProfiles").document(dogId)
                        .set(dogProfile, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Image URL updated in DogProfiles");
                            dogProfile.dogId = dogId;
                            updateDogProfileEverywhere(dogProfile);
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

    /**
     * Interface for handling image upload callbacks.
     */
    public interface OnImageUploadListener {
        /**
         * Called when image upload is successful.
         *
         * @param imageUrl The download URL of the uploaded image
         */
        void onUploadSuccess(String imageUrl);

        /**
         * Called when image upload fails.
         *
         * @param e The exception that caused the failure
         */
        void onUploadFailed(Exception e);
    }

    /**
     * Adds an appointment to both dog and veterinarian collections.
     *
     * @param appointmentId Unique identifier for the appointment
     * @param appointmentData Map containing appointment details
     */
    public static void addAppointment(String appointmentId, Map<String, Object> appointmentData) {
        String dogId = (String) appointmentData.get("dogId");
        String vetId = (String) appointmentData.get("vetId");

        if (dogId != null && !dogId.isEmpty()) {
            db.collection("DogProfiles")
                    .document(dogId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .set(appointmentData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to dog"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to dog", e));
        }

        if (vetId != null && !vetId.isEmpty()) {
            db.collection("Veterinarians")
                    .document(vetId)
                    .collection("Appointments")
                    .document(appointmentId)
                    .set(appointmentData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment added to vet"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add appointment to vet", e));
        }
    }

    /**
     * Deletes an appointment from both dog and veterinarian collections.
     *
     * @param appointmentId Unique identifier for the appointment
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     */
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

    /**
     * Completely deletes an appointment from all locations with callback support.
     *
     * @param appointmentId Unique identifier for the appointment
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     * @param onSuccess Callback to run on successful deletion
     * @param onFailure Callback to run on deletion failure
     */
    public static void deleteAppointmentCompletely(String appointmentId, String dogId, String vetId,
                                                   Runnable onSuccess,
                                                   java.util.function.Consumer<String> onFailure) {
        Log.d(TAG, "Starting complete deletion of appointment: " + appointmentId);

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

    /**
     * Adds a reminder to a user's Reminders subcollection.
     *
     * @param userId User's unique identifier
     * @param reminderId Reminder's unique identifier
     * @param reminderData Map containing reminder details
     */
    public static void addReminderToUser(@NonNull String userId, @NonNull String reminderId, Map<String, Object> reminderData) {
        db.collection("Users").document(userId)
                .collection("Reminders").document(reminderId)
                .set(reminderData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reminder added to user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add reminder to user", e));
    }

    /**
     * Completely deletes a user and all associated data from the system.
     * This includes profile images, dog profiles, appointments, and authentication data.
     *
     * @param userId User's unique identifier
     * @param onSuccess Callback to run on successful deletion
     * @param onFailure Callback to run on deletion failure
     */
    public static void deleteUserCompletely(String userId, Runnable onSuccess, Runnable onFailure) {
        Log.d(TAG, "Starting deletion process for user: " + userId);

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Log.e(TAG, "User document not found: " + userId);
                        if (onFailure != null) onFailure.run();
                        return;
                    }

                    List<Task<Void>> deleteTasks = new ArrayList<>();

                    db.collection("Users").document(userId).collection("Dogs").get()
                            .addOnSuccessListener(dogSnapshots -> {
                                Log.d(TAG, "Found " + dogSnapshots.size() + " dogs to delete");

                                List<String> dogIds = new ArrayList<>();

                                List<Task<Void>> deleteImageTasks = new ArrayList<>();

                                for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                                    String dogId = dogDoc.getString("dogId");
                                    if (dogId != null && !dogId.isEmpty()) {
                                        dogIds.add(dogId);

                                        StorageReference imageRef = storage.getReference()
                                                .child("dog_profile_images/" + dogId + ".jpg");
                                        Task<Void> deleteImageTask = imageRef.delete();
                                        deleteImageTasks.add(deleteImageTask);
                                    }
                                }

                                Boolean isVet = userDoc.getBoolean("isVet");
                                if (isVet != null && isVet) {
                                    StorageReference vetImageRef = storage.getReference()
                                            .child("vet_profile_images/" + userId + ".jpg");
                                    Task<Void> deleteVetImageTask = vetImageRef.delete();
                                    deleteImageTasks.add(deleteVetImageTask);
                                }

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

    /**
     * Helper method for deleting user data after image deletion is complete.
     *
     * @param userId User's unique identifier
     * @param dogIds List of dog IDs associated with the user
     * @param onSuccess Callback to run on successful deletion
     * @param onFailure Callback to run on deletion failure
     */
    private static void deleteUserData(String userId, List<String> dogIds, Runnable onSuccess, Runnable onFailure) {
        Log.d(TAG, "Starting data deletion for user: " + userId + " with " + dogIds.size() + " dogs");

        List<Task<Void>> deleteTasks = new ArrayList<>();

        for (String dogId : dogIds) {
            db.collection("DogProfiles").document(dogId).collection("Appointments").get()
                    .addOnSuccessListener(appointments -> {
                        for (QueryDocumentSnapshot appointment : appointments) {
                            String appointmentId = appointment.getId();
                            String vetId = appointment.getString("vetId");

                            Task<Void> deleteAppointmentTask = appointment.getReference().delete();
                            deleteTasks.add(deleteAppointmentTask);

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

            Task<Void> deleteDogTask = db.collection("DogProfiles").document(dogId).delete();
            deleteTasks.add(deleteDogTask);
            Log.d(TAG, "Added task to delete dog: " + dogId);
        }

        db.collection("Users").document(userId).collection("Reminders").get()
                .addOnSuccessListener(reminders -> {
                    for (QueryDocumentSnapshot reminder : reminders) {
                        Task<Void> deleteReminderTask = reminder.getReference().delete();
                        deleteTasks.add(deleteReminderTask);
                    }
                });

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean isVet = userDoc.getBoolean("isVet");
                    if (isVet != null && isVet) {
                        db.collection("Veterinarians").document(userId).collection("Appointments").get()
                                .addOnSuccessListener(appointments -> {
                                    for (QueryDocumentSnapshot appointment : appointments) {
                                        String appointmentId = appointment.getId();
                                        String dogId = appointment.getString("dogId");

                                        Task<Void> deleteAppointmentTask = appointment.getReference().delete();
                                        deleteTasks.add(deleteAppointmentTask);

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

                        Task<Void> deleteVetTask = db.collection("Veterinarians").document(userId).delete();
                        deleteTasks.add(deleteVetTask);
                    }

                    db.collection("Users").document(userId).collection("Dogs").get()
                            .addOnSuccessListener(dogsCollection -> {
                                for (QueryDocumentSnapshot dogDoc : dogsCollection) {
                                    Task<Void> deleteDogRefTask = dogDoc.getReference().delete();
                                    deleteTasks.add(deleteDogRefTask);
                                }

                                Tasks.whenAllComplete(deleteTasks)
                                        .addOnSuccessListener(results -> {
                                            Log.d(TAG, "All delete tasks completed. Results: " + results.size());

                                            int errorCount = 0;
                                            for (Task task : results) {
                                                if (!task.isSuccessful()) {
                                                    errorCount++;
                                                }
                                            }
                                            Log.d(TAG, "Tasks completed with " + errorCount + " errors");

                                            db.collection("Users").document(userId).delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "User document deleted successfully");

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
     * Updates a dog profile across all relevant collections in the system.
     * This includes DogProfiles, user's Dogs subcollection, veterinarian's Patients, and chat information.
     *
     * @param dogProfile The DogProfile object containing updated information
     */
    public static void updateDogProfileEverywhere(DogProfile dogProfile) {
        if (dogProfile == null || dogProfile.dogId == null) return;
        String dogId = dogProfile.dogId;
        String ownerId = dogProfile.ownerId;
        String vetId = dogProfile.vetId;
        String name = dogProfile.name;
        String imageUrl = dogProfile.profileImageUrl;

        db.collection("DogProfiles").document(dogId)
                .set(dogProfile, SetOptions.merge());

        if (ownerId != null) {
            db.collection("Users").document(ownerId)
                    .collection("Dogs").document(dogId)
                    .set(dogProfile, SetOptions.merge());
        }

        if (vetId != null) {
            db.collection("Veterinarians").document(vetId)
                    .collection("Patients").document(dogId)
                    .set(dogProfile, SetOptions.merge());
        }

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

    /**
     * Marks an appointment as completed across all relevant collections.
     *
     * @param context Application context
     * @param appointmentId Appointment's unique identifier
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     * @param onSuccess Callback to run on successful completion
     * @param onError Callback to run on error with error message
     */
    public static void markAppointmentCompletedEverywhere(android.content.Context context, String appointmentId, String dogId, String vetId, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Veterinarians")
                .document(vetId)
                .collection("Appointments")
                .document(appointmentId)
                .update("completed", true)
                .addOnFailureListener(e -> {
                    if (onError != null) onError.accept("Vet: " + e.getMessage());
                });

        db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .document(appointmentId)
                .update("completed", true)
                .addOnFailureListener(e -> {
                    if (onError != null) onError.accept("Dog: " + e.getMessage());
                });

        db.collection("appointments")
                .document(appointmentId)
                .update("completed", true)
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onError != null) onError.accept("Global: " + e.getMessage());
                });
    }
}