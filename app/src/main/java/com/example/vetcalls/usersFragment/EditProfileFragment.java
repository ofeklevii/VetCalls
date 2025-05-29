package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.obj.Veterinarian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private String downloadUrl = null; // Store download URL for the image

    private Spinner vetSpinner;
    private List<String> vetNames = new ArrayList<>();
    private Map<String, String> vetNameToId = new HashMap<>();
    private String selectedVetId = null;
    private String selectedVetName = null;
    private long lastVetChange = 0L; // Per-dog, loaded from Firestore
    private String originalVetId = null;

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

        vetSpinner = view.findViewById(R.id.vetSpinner);
        loadVetList();

        // Get SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Immediately clear all fields
        clearAllFields();

        // Get dog ID and image URL from arguments
        if (getArguments() != null) {
            dogId = getArguments().getString("dogId", "");
            // קבלת כתובת התמונה מהארגומנטים
            String imageUrl = getArguments().getString("imageUrl", "");
            Log.d(TAG, "Received dogId from arguments: " + dogId);
            Log.d(TAG, "Received imageUrl from arguments: " + imageUrl);

            // אם יש כתובת תמונה בארגומנטים, טען אותה מיד
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadUrl = imageUrl; // שומר את הכתובת
                loadProfileImage(editProfilePic, imageUrl);
            }
        } else {
            // Otherwise get from SharedPreferences
            dogId = sharedPreferences.getString("dogId", "");
            Log.d(TAG, "Using dogId from SharedPreferences: " + dogId);

            // ניסיון לקבל כתובת תמונה מהשייירדפרפרנס
            String savedImageUrl = sharedPreferences.getString("profileImageUrl", null);
            if (savedImageUrl == null || savedImageUrl.isEmpty()) {
                savedImageUrl = sharedPreferences.getString("imageUrl", null);
            }
            if (savedImageUrl != null && !savedImageUrl.isEmpty()) {
                downloadUrl = savedImageUrl;
                loadProfileImage(editProfilePic, savedImageUrl);
            }
        }

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

    private void loadVetList() {
        db.collection("Veterinarians")
                .get()
                .addOnSuccessListener(query -> {
                    vetNames.clear();
                    vetNameToId.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Veterinarian vet = doc.toObject(Veterinarian.class);
                        String name = vet != null ? vet.fullName : null;
                        String id = doc.getId();
                        if (name != null) {
                            vetNames.add(name);
                            vetNameToId.put(name, id);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vetNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    vetSpinner.setAdapter(adapter);

                    // Restore selection if exists
                    String savedVetName = sharedPreferences.getString("vetName", null);
                    if (savedVetName != null && vetNames.contains(savedVetName)) {
                        vetSpinner.setSelection(vetNames.indexOf(savedVetName));
                    }

                    vetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedVetName = vetNames.get(position);
                            selectedVetId = vetNameToId.get(selectedVetName);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedVetName = null;
                            selectedVetId = null;
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading vet list: " + e.getMessage()));
    }

    // פונקציה חדשה לטעינת תמונת פרופיל בצורה אחידה
    private void loadProfileImage(ImageView imageView, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .error(R.drawable.user_person_profile_avatar_icon_190943)
                    .circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image: " + (e != null ? e.getMessage() : "unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }
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
                        // שליפת שדות ידנית עם המרה בטוחה
                        String name = documentSnapshot.getString("name");
                        String birthday = documentSnapshot.getString("birthday");
                        String weight = extractStringOrNumber(documentSnapshot, "weight", "");
                        String race = documentSnapshot.getString("race");
                        String allergies = documentSnapshot.getString("allergies");
                        String vaccines = documentSnapshot.getString("vaccines");
                        String vetId = documentSnapshot.getString("vetId");
                        String vetName = documentSnapshot.getString("vetName");
                        String age = extractStringOrNumber(documentSnapshot, "age", "0");
                        // עדכון שדות במסך
                        editName.setText(name);
                        editBirthday.setText(birthday);
                        editWeight.setText(weight);
                        editRace.setText(race);
                        editAllergies.setText(allergies);
                        editVaccines.setText(vaccines);
                        // שמור מזהי וטרינר
                        selectedVetId = vetId;
                        selectedVetName = vetName;
                        int position = vetNames.indexOf(vetName);
                        if (position != -1) {
                            vetSpinner.setSelection(position);
                        }
                        // הגנה על המרת age ל-Long
                        try {
                            lastVetChange = Long.parseLong(age);
                        } catch (NumberFormatException e) {
                            lastVetChange = 0L;
                        }
                        Log.d(TAG, "Loaded lastVetChange from Firestore: " + lastVetChange + " for dog: " + dogId);
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        Log.d(TAG, "Loaded image URL: " + imageUrl);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            loadProfileImage(editProfilePic, imageUrl);
                            downloadUrl = imageUrl;
                        }
                        Log.d(TAG, "Dog data loaded successfully");
                        originalVetId = vetId;
                    } else {
                        Log.d(TAG, "Dog document doesn't exist");
                        lastVetChange = 0L;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog data: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dog data", Toast.LENGTH_SHORT).show();
                });
    }

    // Save profile
    private void saveProfile() {
        Log.d(TAG, "Starting to save profile...");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Please fill in all required fields (name and weight)", Toast.LENGTH_SHORT).show();
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
        String bio = buildBio(weight, allergies, vaccines, race, birthday);

        // בדיקה אם וטרינר נבחר
        if (selectedVetId == null || selectedVetName == null) {
            Toast.makeText(requireContext(), "Please select a vet to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        // הגבלת שינוי פעם בשבוע
        long now = System.currentTimeMillis();
        long oneWeekMillis = 7 * 24 * 60 * 60 * 1000;
        boolean vetChanged = originalVetId != null && !originalVetId.equals(selectedVetId);
        if (!isNewDog && vetChanged && lastVetChange != 0 && (now - lastVetChange) < oneWeekMillis) {
            Log.d(TAG, "Vet change rejected - Less than a week since last change");
            Toast.makeText(requireContext(), "Vet can only be changed once a week", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Vet change allowed - Proceeding with save");

        // יצירת אובייקט DogProfile
        DogProfile dogProfile = new DogProfile();
        dogProfile.dogId = dogId;
        dogProfile.name = name;
        dogProfile.vetId = selectedVetId;
        dogProfile.vetName = selectedVetName;
        dogProfile.weight = weight;
        dogProfile.allergies = allergies;
        dogProfile.vaccines = vaccines;
        dogProfile.bio = bio;
        dogProfile.ownerId = ownerId;
        dogProfile.lastUpdated = System.currentTimeMillis();
        dogProfile.lastVetChange = now;
        dogProfile.race = race;
        dogProfile.birthday = birthday;
        dogProfile.age = ageStr;
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            dogProfile.profileImageUrl = downloadUrl;
        } else {
            dogProfile.profileImageUrl = ""; // תמונה ריקה אם לא נבחרה
        }

        Log.d(TAG, "Saving profile with dogId: " + dogId);

        // עדכון גלובלי בכל המקומות
        com.example.vetcalls.obj.FirestoreUserHelper.updateDogProfileEverywhere(dogProfile);

        // שמירה גם בתת-קולקשן Dogs של המשתמש (כל השדות)
        db.collection("Users").document(ownerId)
            .collection("Dogs").document(dogId)
            .set(dogProfile)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog full data saved to user's Dogs subcollection"))
            .addOnFailureListener(e -> Log.e(TAG, "Error saving dog to user's Dogs subcollection: " + e.getMessage()));

        // Handle image if selected
        if (selectedImageUri != null) {
            uploadImageToFirebase(selectedImageUri);
        } else {
            // Continue process even without new image
            finishSaveProcess(name, ageStr, bio, race, birthday, weight, allergies, vaccines);
        }
    }

    // Build bio from data
    private String buildBio(String weight, String allergies, String vaccines, String race, String birthday) {
        StringBuilder bioBuilder = new StringBuilder();
        if (weight != null && !weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(weight).append(" kg");
        }
        if (race != null && !race.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Race: ").append(race);
        }
        if (birthday != null && !birthday.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Birthday: ").append(birthday);
        }
        if (allergies != null && !allergies.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Allergies: ").append(allergies);
        }
        if (vaccines != null && !vaccines.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
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
        editor.putString("vetId", selectedVetId);
        editor.putString("vetName", selectedVetName);
        editor.putString("bio", bio);
        editor.putString("dogId", dogId);

        // Save image URL in both fields for maximum compatibility
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            editor.putString("profileImageUrl", downloadUrl);
            Log.d(TAG, "Saving image URL to SharedPreferences: " + downloadUrl);
        } else {
            editor.remove("profileImageUrl"); // מסיר תמונה ישנה אם אין חדשה
        }

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
        result.putString("vetId", selectedVetId);
        result.putString("vetName", selectedVetName);
        result.putBoolean("isNewDog", isNewDog); // Important - pass info if this is a new dog

        // If we have a valid image URL, send it
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            result.putString("updatedImageUri", downloadUrl);
            Log.d(TAG, "Sending image URL in result: " + downloadUrl);
        } else if (selectedImageUri != null) {
            // If no download URL but we have selected image URI
            result.putString("updatedImageUri", selectedImageUri.toString());
            Log.d(TAG, "Sending selected image URI in result: " + selectedImageUri);
        }

        Log.d(TAG, "Setting fragment result with dogId: " + dogId);
        getParentFragmentManager().setFragmentResult("editProfileKey", result);

        Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
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
                // שימוש בפונקציה החדשה לטעינת תמונה
                loadProfileImage(editProfilePic, selectedImageUri.toString());
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show();
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

    // Upload image to Firebase Storage - שיפור הטיפול בתמונה
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            // If no image, continue the process
            finishSaveProcess(editName.getText().toString().trim(),
                    calculateDogAge(editBirthday.getText().toString().trim()),
                    buildBio(editWeight.getText().toString().trim(),
                            editAllergies.getText().toString().trim(),
                            editVaccines.getText().toString().trim(),
                            editRace.getText().toString().trim(),
                            editBirthday.getText().toString().trim()),
                    editRace.getText().toString().trim(),
                    editBirthday.getText().toString().trim(),
                    editWeight.getText().toString().trim(),
                    editAllergies.getText().toString().trim(),
                    editVaccines.getText().toString().trim());
            return;
        }

        Log.d(TAG, "Uploading image to Firebase Storage, URI: " + imageUri);

        // Create a more specific reference with folder structure
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("dog_profile_images")
                .child(dogId + ".jpg");

        Log.d(TAG, "Storage reference path: " + storageRef.getPath());

        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Uploading image...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");

                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Store the download URL
                        downloadUrl = uri.toString();
                        Log.d(TAG, "Got download URL: " + downloadUrl);

                        // Update both image fields in Firestore for maximum compatibility
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", downloadUrl);

                        db.collection("DogProfiles")
                                .document(dogId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Image URLs updated in Firestore");

                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }

                                    // Save in SharedPreferences both fields
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("profileImageUrl", downloadUrl);
                                    editor.apply();

                                    // Continue save process after updating image
                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(),
                                                    editAllergies.getText().toString().trim(),
                                                    editVaccines.getText().toString().trim(),
                                                    editRace.getText().toString().trim(),
                                                    editBirthday.getText().toString().trim()),
                                            editRace.getText().toString().trim(),
                                            editBirthday.getText().toString().trim(),
                                            editWeight.getText().toString().trim(),
                                            editAllergies.getText().toString().trim(),
                                            editVaccines.getText().toString().trim());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating image URLs in Firestore: " + e.getMessage());

                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }

                                    // Continue process despite error
                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(),
                                                    editAllergies.getText().toString().trim(),
                                                    editVaccines.getText().toString().trim(),
                                                    editRace.getText().toString().trim(),
                                                    editBirthday.getText().toString().trim()),
                                            editRace.getText().toString().trim(),
                                            editBirthday.getText().toString().trim(),
                                            editWeight.getText().toString().trim(),
                                            editAllergies.getText().toString().trim(),
                                            editVaccines.getText().toString().trim());
                                });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());

                        if (loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }

                        // Continue process despite error
                        finishSaveProcess(editName.getText().toString().trim(),
                                calculateDogAge(editBirthday.getText().toString().trim()),
                                buildBio(editWeight.getText().toString().trim(),
                                        editAllergies.getText().toString().trim(),
                                        editVaccines.getText().toString().trim(),
                                        editRace.getText().toString().trim(),
                                        editBirthday.getText().toString().trim()),
                                editRace.getText().toString().trim(),
                                editBirthday.getText().toString().trim(),
                                editWeight.getText().toString().trim(),
                                editAllergies.getText().toString().trim(),
                                editVaccines.getText().toString().trim());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());

                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }

                    Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Continue process despite error
                    finishSaveProcess(editName.getText().toString().trim(),
                            calculateDogAge(editBirthday.getText().toString().trim()),
                            buildBio(editWeight.getText().toString().trim(),
                                    editAllergies.getText().toString().trim(),
                                    editVaccines.getText().toString().trim(),
                                    editRace.getText().toString().trim(),
                                    editBirthday.getText().toString().trim()),
                            editRace.getText().toString().trim(),
                            editBirthday.getText().toString().trim(),
                            editWeight.getText().toString().trim(),
                            editAllergies.getText().toString().trim(),
                            editVaccines.getText().toString().trim());
                })
                .addOnProgressListener(snapshot -> {
                    // Calculate progress percentage
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
    }

    private String extractStringOrNumber(DocumentSnapshot document, String field, String defaultValue) {
        Object obj = document.get(field);
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Number) {
            return String.valueOf(obj);
        } else {
            return defaultValue;
        }
    }
}