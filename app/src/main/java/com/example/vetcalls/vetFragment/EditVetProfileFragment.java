package com.example.vetcalls.vetFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.obj.Veterinarian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for editing veterinarian profile information including personal details and profile image.
 * Handles data validation, image upload, and synchronization with Firestore database.
 *
 * @author Ofek Levi
 */
public class EditVetProfileFragment extends Fragment {

    /** Tag for logging purposes */
    private static final String TAG = "EditVetProfileFragment";

    /** Request code for image selection from gallery */
    private static final int REQUEST_IMAGE_PICK = 1;

    /** Request code for camera image capture */
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    /** Request code for permissions */
    private static final int PERMISSIONS_REQUEST_CODE = 101;

    /** ImageView for displaying and changing profile picture */
    private ImageView profileImage;

    /** EditText fields for veterinarian profile information */
    private EditText editFullName, editClinicAddress, editWorkHoursFirstPart, editWorkHoursSecondPart, editWorkHoursThirdPart, editPhoneNumber;

    /** Bitmap of the selected profile image */
    private Bitmap selectedImageBitmap;

    /** URI of the selected image for direct upload */
    private Uri selectedImageUri;

    /** Loading dialog displayed during upload operations */
    private AlertDialog loadingDialog;

    /** Flag indicating if an upload operation is in progress */
    private boolean isUploading = false;

    /** Current profile image URL stored in database */
    private String currentProfileImageUrl = null;

    /** Firebase Firestore database instance */
    private FirebaseFirestore db;

    /** Firebase authentication instance */
    private FirebaseAuth auth;

    /** SharedPreferences for local data storage */
    private SharedPreferences sharedPreferences;

    /**
     * Creates and initializes the fragment view with all UI components and Firebase services.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment's UI will be attached to
     * @param savedInstanceState Bundle containing the fragment's previously saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_vet_profile, container, false);

        initializeFirebase();
        initializeUI(view);

        loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Uploading image...")
                .setCancelable(false)
                .create();

        loadExistingData();

        return view;
    }

    /**
     * Initializes Firebase services and SharedPreferences.
     */
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("VetProfile", Context.MODE_PRIVATE);
    }

    /**
     * Initializes all UI components and sets up button click listeners.
     *
     * @param view The root view of the fragment
     */
    private void initializeUI(View view) {
        profileImage = view.findViewById(R.id.editProfileImage);
        editFullName = view.findViewById(R.id.editFullName);
        editClinicAddress = view.findViewById(R.id.editClinicAddress);
        editWorkHoursFirstPart = view.findViewById(R.id.editWorkHoursFirstPart);
        editWorkHoursSecondPart = view.findViewById(R.id.editWorkHoursSecondPart);
        editWorkHoursThirdPart = view.findViewById(R.id.editWorkHoursThirdPart);
        editPhoneNumber = view.findViewById(R.id.editPhoneNumber);

        view.findViewById(R.id.changeProfileImageButton).setOnClickListener(v -> {
            if (checkPermissions()) showImagePickerDialog();
            else requestPermissions();
        });

        view.findViewById(R.id.saveButton).setOnClickListener(v -> saveProfileChanges());
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> navigateToVetHome());
    }

    /**
     * Checks if required permissions for camera and storage access are granted.
     *
     * @return true if all required permissions are granted, false otherwise
     */
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests required permissions for camera and storage access.
     */
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissions.add(android.Manifest.permission.CAMERA);

        if (!permissions.isEmpty())
            requestPermissions(permissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode The request code passed to requestPermissions
     * @param permissions The requested permissions
     * @param grantResults The grant results for the requested permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) showImagePickerDialog();
            else Toast.makeText(requireContext(), "Permissions required to change profile image", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Loads existing veterinarian data from Firestore and populates the form fields.
     */
    private void loadExistingData() {
        try {
            String vetId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (vetId != null) {
                db.collection("Veterinarians").document(vetId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                populateFieldsFromDocument(documentSnapshot);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading vet data from Firestore", e);
        }
    }

    /**
     * Populates form fields with data from Firestore document.
     *
     * @param documentSnapshot The Firestore document containing veterinarian data
     */
    private void populateFieldsFromDocument(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        String fullName = documentSnapshot.getString("fullName");
        String clinicAddress = documentSnapshot.getString("clinicAddress");
        String workHoursFirstPart = documentSnapshot.getString("workHoursFirstPart");
        String workHoursSecondPart = documentSnapshot.getString("workHoursSecondPart");
        String workHoursThirdPart = documentSnapshot.getString("workHoursThirdPart");
        String phoneNumber = documentSnapshot.getString("phoneNumber");
        String imageUrl = documentSnapshot.getString("profileImageUrl");

        if (fullName != null) editFullName.setText(fullName);
        if (clinicAddress != null) editClinicAddress.setText(clinicAddress);
        if (workHoursFirstPart != null) editWorkHoursFirstPart.setText(workHoursFirstPart);
        if (workHoursSecondPart != null) editWorkHoursSecondPart.setText(workHoursSecondPart);
        if (workHoursThirdPart != null) editWorkHoursThirdPart.setText(workHoursThirdPart);
        if (phoneNumber != null) editPhoneNumber.setText(phoneNumber);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            currentProfileImageUrl = imageUrl;
            loadProfileImage(imageUrl);
        }
    }

    /**
     * Loads a profile image into the ImageView using Glide with caching and error handling.
     *
     * @param imageUrl The URL of the image to load
     */
    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image: " + imageUrl);

            Glide.with(this)
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
                            Log.d(TAG, "Image loaded successfully from URL: " + imageUrl);
                            return false;
                        }
                    })
                    .into(profileImage);
        } else {
            Log.d(TAG, "No image URL provided, setting default image");
            profileImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }
    }

    /**
     * Shows a dialog allowing the user to choose between gallery and camera for image selection.
     */
    private void showImagePickerDialog() {
        String[] options = {"Choose from the gallery", "Take a picture"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose a profile picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(intent, REQUEST_IMAGE_PICK);
                    } else {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    /**
     * Handles the result from image selection or camera capture activities.
     *
     * @param requestCode The request code used to start the activity
     * @param resultCode The result code returned by the activity
     * @param data The intent data containing the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_IMAGE_PICK) {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    }
                } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        selectedImageBitmap = (Bitmap) extras.get("data");
                        selectedImageUri = getImageUriFromBitmap(selectedImageBitmap);
                    }
                }

                if (selectedImageBitmap != null) {
                    Glide.with(this)
                            .load(selectedImageBitmap)
                            .circleCrop()
                            .into(profileImage);

                    Log.d(TAG, "Image selected and displayed in the UI");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
                Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Converts a Bitmap to URI by saving it to the device's media store.
     *
     * @param bitmap The bitmap image to convert
     * @return URI of the saved image
     */
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(
                requireContext().getContentResolver(), bitmap, "ProfileImage", null);
        return Uri.parse(path);
    }

    /**
     * Validates form fields and initiates the profile save process.
     * Checks for required fields and prevents duplicate save operations.
     */
    private void saveProfileChanges() {
        if (isUploading) {
            Toast.makeText(requireContext(), "Profile update already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = editFullName.getText().toString().trim();
        String clinicAddress = editClinicAddress.getText().toString().trim();
        String workHoursFirstPart = editWorkHoursFirstPart.getText().toString().trim();
        String workHoursSecondPart = editWorkHoursSecondPart.getText().toString().trim();
        String workHoursThirdPart = editWorkHoursThirdPart.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();

        if (fullName.isEmpty() || clinicAddress.isEmpty() ||
                workHoursFirstPart.isEmpty() || workHoursSecondPart.isEmpty() || workHoursThirdPart.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";

        Veterinarian profileData = new Veterinarian(
                fullName,
                clinicAddress,
                workHoursFirstPart,
                workHoursSecondPart,
                workHoursThirdPart,
                currentProfileImageUrl,
                email,
                phoneNumber,
                true,
                auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null
        );

        isUploading = true;

        loadingDialog.setMessage(selectedImageBitmap != null ? "Uploading image..." : "Updating profile...");
        loadingDialog.show();

        updateFirestoreData(profileData);
    }

    /**
     * Updates veterinarian data in Firestore database.
     *
     * @param profileData The Veterinarian object containing updated profile data
     */
    private void updateFirestoreData(Veterinarian profileData) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            isUploading = false;
            if (loadingDialog.isShowing()) loadingDialog.dismiss();
            return;
        }

        Map<String, Object> updates = createUpdateMap(profileData);

        String vetId = auth.getCurrentUser().getUid();
        db.collection("Veterinarians").document(vetId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile data updated in Firestore successfully");

                    updateChatsWithVetProfile(vetId, profileData.fullName, profileData.profileImageUrl);

                    saveBasicDataToSharedPreferences(profileData);

                    if (selectedImageUri != null) {
                        uploadProfileImageUsingHelper(selectedImageUri, profileData);
                    } else {
                        finishProfileUpdate(profileData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update profile in Firestore", e);
                    Toast.makeText(requireContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isUploading = false;
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                });
    }

    /**
     * Creates a map of updates for Firestore from the profile data.
     *
     * @param profileData The Veterinarian object containing profile data
     * @return Map containing the update fields and values
     */
    private Map<String, Object> createUpdateMap(Veterinarian profileData) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", profileData.fullName);
        updates.put("clinicAddress", profileData.clinicAddress);
        updates.put("workHoursFirstPart", profileData.workHoursFirstPart);
        updates.put("workHoursSecondPart", profileData.workHoursSecondPart);
        updates.put("workHoursThirdPart", profileData.workHoursThirdPart);
        updates.put("email", profileData.email);
        updates.put("phoneNumber", profileData.phoneNumber);
        updates.put("profileImageUrl", profileData.profileImageUrl);
        updates.put("isVet", true);
        updates.put("uid", profileData.uid);
        return updates;
    }

    /**
     * Saves basic profile data to SharedPreferences for local access.
     *
     * @param profileData The Veterinarian object containing profile data
     */
    private void saveBasicDataToSharedPreferences(Veterinarian profileData) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fullName", profileData.fullName);
        editor.putString("clinicAddress", profileData.clinicAddress);
        editor.putString("workHoursFirstPart", profileData.workHoursFirstPart);
        editor.putString("workHoursSecondPart", profileData.workHoursSecondPart);
        editor.putString("workHoursThirdPart", profileData.workHoursThirdPart);
        editor.putString("email", profileData.email);
        editor.putString("phoneNumber", profileData.phoneNumber);

        Gson gson = new Gson();
        String profileJson = gson.toJson(profileData);
        editor.putString("vet_profile_json", profileJson);

        editor.apply();
        Log.d(TAG, "Basic profile data saved to SharedPreferences");
    }

    /**
     * Uploads profile image using FirestoreUserHelper utility class.
     *
     * @param imageUri The URI of the image to upload
     * @param profileData The Veterinarian object to update with new image URL
     */
    private void uploadProfileImageUsingHelper(Uri imageUri, Veterinarian profileData) {
        String vetId = auth.getCurrentUser().getUid();

        FirestoreUserHelper.uploadVetProfileImage(imageUri, vetId, new FirestoreUserHelper.OnImageUploadListener() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                Log.d(TAG, "Image uploaded successfully via FirestoreUserHelper. URL: " + imageUrl);

                profileData.profileImageUrl = imageUrl;
                currentProfileImageUrl = imageUrl;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("profileImageUrl", imageUrl);
                editor.apply();

                finishProfileUpdate(profileData);
            }

            @Override
            public void onUploadFailed(Exception e) {
                Log.e(TAG, "Failed to upload image via FirestoreUserHelper", e);
                Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                finishProfileUpdate(profileData);
            }
        });
    }

    /**
     * Completes the profile update process by saving data and updating UI.
     *
     * @param profileData The final Veterinarian object with all updated data
     */
    private void finishProfileUpdate(Veterinarian profileData) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (profileData.profileImageUrl != null) {
                editor.putString("profileImageUrl", profileData.profileImageUrl);
                Log.d(TAG, "Saving final image URL to SharedPreferences: " + profileData.profileImageUrl);
            }

            Gson gson = new Gson();
            String profileJson = gson.toJson(profileData);
            editor.putString("vet_profile_json", profileJson);

            editor.commit();

            Log.d(TAG, "Profile data saved to SharedPreferences as JSON");

            updateParentFragment();

            saveToFirestoreAgain(profileData);

        } catch (Exception e) {
            Log.e(TAG, "Error saving profile data", e);
        }

        isUploading = false;
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
        navigateToVetHome();
    }

    /**
     * Attempts to update the parent VetHomeFragment if it exists.
     */
    private void updateParentFragment() {
        try {
            Fragment parentFragment = getParentFragment();
            if (parentFragment != null && parentFragment.getClass().getSimpleName().equals("VetHomeFragment")) {
                java.lang.reflect.Method updateMethod = parentFragment.getClass().getDeclaredMethod("updateProfileView");
                updateMethod.setAccessible(true);
                updateMethod.invoke(parentFragment);
                Log.d(TAG, "Called updateProfileView() on VetHomeFragment");
            }
        } catch (Exception e) {
            Log.e(TAG, "Couldn't update VetHomeFragment", e);
        }
    }

    /**
     * Saves all profile data to Firestore again to ensure consistency.
     *
     * @param profileData The Veterinarian object to save
     */
    private void saveToFirestoreAgain(Veterinarian profileData) {
        Map<String, Object> updates = createUpdateMap(profileData);
        String vetId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (vetId != null) {
            db.collection("Veterinarians").document(vetId)
                    .set(updates, SetOptions.merge());
        }
    }

    /**
     * Navigates back to the VetHomeFragment.
     */
    private void navigateToVetHome() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new com.example.vetcalls.vetFragment.VetHomeFragment())
                .commit();
    }

    /**
     * Updates all chat documents where this veterinarian is involved with new profile information.
     *
     * @param vetId The veterinarian's unique identifier
     * @param newName The updated veterinarian name
     * @param newImageUrl The updated profile image URL
     */
    private void updateChatsWithVetProfile(String vetId, String newName, String newImageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Chats")
                .whereEqualTo("vetId", vetId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot chatDoc : querySnapshot.getDocuments()) {
                        chatDoc.getReference().update("vetName", newName, "vetImageUrl", newName != null && !newName.trim().isEmpty() ? newImageUrl : "https://example.com/default_vet_image.png");
                    }
                });
    }
}