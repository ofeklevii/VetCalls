package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditProfileFragment extends Fragment {
    private static final String TAG = "EditProfileFragment";
    private EditText editName, editBirthday, editWeight, editRace, editAllergies, editVaccines;
    private Button saveButton, changeProfilePicButton, cancelButton;
    private ImageView editProfilePic;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Uri selectedImageUri;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private String dogId;
    private boolean isNewDog = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize UI components
        editName = view.findViewById(R.id.editName);
        editBirthday = view.findViewById(R.id.editBirthday);
        editWeight = view.findViewById(R.id.editWeight);
        editRace = view.findViewById(R.id.editRace);
        editAllergies = view.findViewById(R.id.editAllergies);
        editVaccines = view.findViewById(R.id.editVaccines);
        saveButton = view.findViewById(R.id.saveButton);
        changeProfilePicButton = view.findViewById(R.id.changeProfilePicButton);
        editProfilePic = view.findViewById(R.id.editProfilePic);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Get dog ID from arguments
        if (getArguments() != null) {
            dogId = getArguments().getString("dogId", "");
            Log.d(TAG, "Received dogId from arguments: " + dogId);
        } else {
            // Otherwise get from SharedPreferences
            dogId = sharedPreferences.getString("dogId", "");
            Log.d(TAG, "Using dogId from SharedPreferences: " + dogId);
        }

        // Immediately clear all fields
        clearAllFields();

        // Check if this is a new dog
        isNewDog = (dogId == null || dogId.isEmpty());
        Log.d(TAG, "Is this a new dog? " + isNewDog);

        // If there's a dog ID, load its data
        if (!isNewDog) {
            loadDogDataFromFirestore(dogId);
        }

        // Set up button listeners
        saveButton.setOnClickListener(v -> saveProfile());
        changeProfilePicButton.setOnClickListener(v -> showImagePickerDialog());
        cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    // Clear all fields
    private void clearAllFields() {
        editName.setText("");
        editBirthday.setText("");
        editWeight.setText("");
        editRace.setText("");
        editAllergies.setText("");
        editVaccines.setText("");
        editProfilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
    }

    // Load dog data from Firestore
    private void loadDogDataFromFirestore(String dogId) {
        Log.d(TAG, "Loading dog data for id: " + dogId);

        if (dogId == null || dogId.isEmpty()) {
            return;
        }

        db.collection("DogProfiles").document(dogId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Dog document exists");

                        // Check if dog belongs to current user
                        String ownerId = documentSnapshot.getString("ownerId");
                        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

                        if (currentUserId != null && ownerId != null && !currentUserId.equals(ownerId)) {
                            // Dog doesn't belong to current user
                            Log.w(TAG, "Dog belongs to a different user");
                            return;
                        }

                        // Load data
                        String name = documentSnapshot.getString("name");
                        String birthday = documentSnapshot.getString("birthday");
                        String weight = documentSnapshot.getString("weight");
                        String race = documentSnapshot.getString("race");
                        String allergies = documentSnapshot.getString("allergies");
                        String vaccines = documentSnapshot.getString("vaccines");

                        // Try to get image URL from two possible fields
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        if (imageUrl == null) {
                            imageUrl = documentSnapshot.getString("imageUrl");
                        }

                        // Update fields
                        editName.setText(name != null ? name : "");
                        editBirthday.setText(birthday != null ? birthday : "");
                        editWeight.setText(weight != null ? weight : "");
                        editRace.setText(race != null ? race : "");
                        editAllergies.setText(allergies != null ? allergies : "");
                        editVaccines.setText(vaccines != null ? vaccines : "");

                        // Load profile picture
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(editProfilePic);
                        }

                        Log.d(TAG, "Dog data loaded successfully");
                    } else {
                        Log.d(TAG, "Dog document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog data: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dog data", Toast.LENGTH_SHORT).show();
                });
    }

    // Save profile - corrected method
    private void saveProfile() {
        Log.d(TAG, "Starting to save profile...");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect data from fields
        String name = editName.getText().toString().trim();
        String race = editRace.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();
        String allergies = editAllergies.getText().toString().trim();
        String vaccines = editVaccines.getText().toString().trim();
        String ownerId = currentUser.getUid();

        // Validate input
        if (name.isEmpty() || weight.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields (name and weight)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate dog age
        String ageStr = calculateDogAge(birthday);
        int age = 0;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Could not parse age as integer: " + ageStr);
        }

        // Check if dog ID exists
        if (dogId == null || dogId.isEmpty()) {
            dogId = UUID.randomUUID().toString();
            isNewDog = true;
            Log.d(TAG, "This is a new dog with ID: " + dogId);
        } else {
            Log.d(TAG, "Updating existing dog ID: " + dogId);
        }

        // Build bio
        String bio = buildBio(weight, allergies, vaccines);

        // Create data map
        Map<String, Object> dogData = new HashMap<>();
        dogData.put("name", name);
        dogData.put("race", race);
        dogData.put("age", age);
        dogData.put("birthday", birthday);
        dogData.put("weight", weight); // Save as string
        dogData.put("allergies", allergies);
        dogData.put("vaccines", vaccines);
        dogData.put("bio", bio);
        dogData.put("ownerId", ownerId);
        dogData.put("lastUpdated", System.currentTimeMillis()); // Add timestamp

        Log.d(TAG, "Saving profile with dogId: " + dogId);

        // Always use set instead of update
        db.collection("DogProfiles").document(dogId)
                .set(dogData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog profile saved successfully to DogProfiles/" + dogId);

                    // If this is a new dog, add it to user's dog collection
                    if (isNewDog) {
                        // Change - use fixed ID instead of auto ID
                        db.collection("Users").document(ownerId)
                                .collection("Dogs").document(dogId)  // Use dog's own ID
                                .set(new HashMap<String, Object>() {{
                                    put("dogId", dogId);
                                    put("name", name);
                                    put("timestamp", System.currentTimeMillis());
                                }})
                                .addOnSuccessListener(unused -> {
                                    Log.d(TAG, "Dog reference added to user's collection with explicit ID: " + dogId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding dog reference: " + e.getMessage());
                                });
                    }

                    // Handle image if selected
                    if (selectedImageUri != null) {
                        uploadImageToFirebase(selectedImageUri);
                    } else {
                        // Continue process even without new image
                        finishSaveProcess(name, ageStr, bio, race, birthday, weight, allergies, vaccines);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving dog profile: " + e.getMessage());
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Build bio from data
    private String buildBio(String weight, String allergies, String vaccines) {
        StringBuilder bioBuilder = new StringBuilder();
        if (weight != null && !weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(weight).append(" kg\n");
        }
        if (allergies != null && !allergies.isEmpty()) {
            bioBuilder.append("Allergies: ").append(allergies).append("\n");
        }
        if (vaccines != null && !vaccines.isEmpty()) {
            bioBuilder.append("Vaccines: ").append(vaccines);
        }
        return bioBuilder.toString().trim();
    }

    // Finish save process
    private void finishSaveProcess(String name, String age, String bio, String race,
                                   String birthday, String weight, String allergies, String vaccines) {
        Log.d(TAG, "Finishing save process");

        // Save in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("birthday", birthday);
        editor.putString("weight", weight);
        editor.putString("race", race);
        editor.putString("allergies", allergies);
        editor.putString("vaccines", vaccines);
        editor.putString("age", age);
        editor.putString("bio", bio);
        editor.putString("dogId", dogId);
        editor.apply();

        Log.d(TAG, "Saved to SharedPreferences, dogId: " + dogId);

        // Update HomeFragment
        Bundle result = new Bundle();
        result.putString("updatedBio", bio);
        result.putString("updatedDogAge", age);
        result.putString("updatedUserName", name);
        result.putString("race", race);
        result.putString("birthday", birthday);
        result.putString("weight", weight);
        result.putString("allergies", allergies);
        result.putString("vaccines", vaccines);
        result.putString("dogId", dogId);
        result.putBoolean("isNewDog", isNewDog); // Important - pass info if this is a new dog

        // If there's a selected image URI, send it too
        if (selectedImageUri != null) {
            result.putString("updatedImageUri", selectedImageUri.toString());
        }

        Log.d(TAG, "Setting fragment result with dogId: " + dogId);
        getParentFragmentManager().setFragmentResult("editProfileKey", result);

        Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Finishing profile save and returning to HomeFragment");

        // Short wait before returning to previous screen to allow Firebase to update
        new android.os.Handler().postDelayed(() -> {
            if (isAdded()) { // Make sure fragment is still attached
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        }, 500);  // Wait half a second
    }

    // Calculate dog age from birth date
    private String calculateDogAge(String birthday) {
        if (birthday == null || birthday.isEmpty()) return "0";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date birthDate = dateFormat.parse(birthday);
            if (birthDate == null) return "0";

            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return String.valueOf(Math.max(0, age));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing birthday: " + e.getMessage());
            return "0";
        }
    }

    // Open dialog to choose image
    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickImageFromGallery();
                    else takePhotoWithCamera();
                })
                .show();
    }

    // Pick image from gallery
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // Take photo with camera
    private void takePhotoWithCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // Handle image selection result
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                selectedImageUri = data.getData();
                Log.d(TAG, "Image selected from gallery: " + selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    selectedImageUri = getImageUriFromBitmap(requireContext(), photo);
                    Log.d(TAG, "Image captured from camera: " + selectedImageUri);
                }
            }

            if (selectedImageUri != null) {
                // Display selected image
                Glide.with(requireContext())
                        .load(selectedImageUri)
                        .circleCrop()
                        .into(editProfilePic);
                Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Convert Bitmap to URI
    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "DogProfile_" + System.currentTimeMillis(), null);
        return path != null ? Uri.parse(path) : null;
    }

    // Upload image to Firebase Storage
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            // If no image, continue the process
            finishSaveProcess(editName.getText().toString().trim(),
                    calculateDogAge(editBirthday.getText().toString().trim()),
                    buildBio(editWeight.getText().toString().trim(),
                            editAllergies.getText().toString().trim(),
                            editVaccines.getText().toString().trim()),
                    editRace.getText().toString().trim(),
                    editBirthday.getText().toString().trim(),
                    editWeight.getText().toString().trim(),
                    editAllergies.getText().toString().trim(),
                    editVaccines.getText().toString().trim());
            return;
        }

        Log.d(TAG, "Uploading image to Firebase Storage");

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("dog_profile_images/" + dogId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");

                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "Image download URL: " + downloadUrl);

                        // Update both image fields in Firestore for maximum compatibility
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", downloadUrl);
                        updates.put("imageUrl", downloadUrl);  // Also this field in case other code uses it

                        db.collection("DogProfiles")
                                .document(dogId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Image URLs updated in Firestore");

                                    // Local save
                                    sharedPreferences.edit()
                                            .putString("profileImageUrl", downloadUrl)
                                            .apply();

                                    // Continue save process after updating image
                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(),
                                                    editAllergies.getText().toString().trim(),
                                                    editVaccines.getText().toString().trim()),
                                            editRace.getText().toString().trim(),
                                            editBirthday.getText().toString().trim(),
                                            editWeight.getText().toString().trim(),
                                            editAllergies.getText().toString().trim(),
                                            editVaccines.getText().toString().trim());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating image URLs in Firestore: " + e.getMessage());
                                    // Continue process despite error
                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(),
                                                    editAllergies.getText().toString().trim(),
                                                    editVaccines.getText().toString().trim()),
                                            editRace.getText().toString().trim(),
                                            editBirthday.getText().toString().trim(),
                                            editWeight.getText().toString().trim(),
                                            editAllergies.getText().toString().trim(),
                                            editVaccines.getText().toString().trim());
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Continue process despite error
                    finishSaveProcess(editName.getText().toString().trim(),
                            calculateDogAge(editBirthday.getText().toString().trim()),
                            buildBio(editWeight.getText().toString().trim(),
                                    editAllergies.getText().toString().trim(),
                                    editVaccines.getText().toString().trim()),
                            editRace.getText().toString().trim(),
                            editBirthday.getText().toString().trim(),
                            editWeight.getText().toString().trim(),
                            editAllergies.getText().toString().trim(),
                            editVaccines.getText().toString().trim());
                });
    }
}