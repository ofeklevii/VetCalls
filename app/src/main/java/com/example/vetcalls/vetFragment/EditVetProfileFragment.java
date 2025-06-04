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

public class EditVetProfileFragment extends Fragment {
    private static final String TAG = "EditVetProfileFragment";
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PERMISSIONS_REQUEST_CODE = 101;

    private ImageView profileImage;
    private EditText editFullName, editClinicAddress, editWorkHoursFirstPart, editWorkHoursSecondPart, editWorkHoursThirdPart, editPhoneNumber;
    private Bitmap selectedImageBitmap;
    private Uri selectedImageUri; // Added to store the URI directly for FirestoreUserHelper
    private AlertDialog loadingDialog;
    private boolean isUploading = false;
    private String currentProfileImageUrl = null; // Keep track of current image URL

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_vet_profile, container, false);

        // אתחול Firebase ו-UI
        initializeFirebase();
        initializeUI(view);

        // יצירת דיאלוג טעינה
        loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Uploading image...")
                .setCancelable(false)
                .create();

        // טעינת נתונים קיימים
        loadExistingData();

        return view;
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("VetProfile", Context.MODE_PRIVATE);
    }

    private void initializeUI(View view) {
        profileImage = view.findViewById(R.id.editProfileImage);
        editFullName = view.findViewById(R.id.editFullName);
        editClinicAddress = view.findViewById(R.id.editClinicAddress);
        editWorkHoursFirstPart = view.findViewById(R.id.editWorkHoursFirstPart);
        editWorkHoursSecondPart = view.findViewById(R.id.editWorkHoursSecondPart);
        editWorkHoursThirdPart = view.findViewById(R.id.editWorkHoursThirdPart);
        editPhoneNumber = view.findViewById(R.id.editPhoneNumber);

        // מאזינים לכפתורים
        view.findViewById(R.id.changeProfileImageButton).setOnClickListener(v -> {
            if (checkPermissions()) showImagePickerDialog();
            else requestPermissions();
        });

        view.findViewById(R.id.saveButton).setOnClickListener(v -> saveProfileChanges());
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> navigateToVetHome());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissions.add(android.Manifest.permission.CAMERA);

        if (!permissions.isEmpty())
            requestPermissions(permissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
    }

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

    private void loadExistingData() {
        try {
            String vetId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (vetId != null) {
                db.collection("Veterinarians").document(vetId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // טען את כל השדות מהמסמך
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
                    });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading vet data from Firestore", e);
        }
    }

    // שיטה משופרת לטעינת תמונת פרופיל
    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image: " + imageUrl);

            Glide.with(this) // Use 'this' fragment instead of context which might be detached
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_IMAGE_PICK) {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri; // Store the URI for direct upload
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    }
                } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        selectedImageBitmap = (Bitmap) extras.get("data");
                        // Convert bitmap to URI for FirestoreUserHelper
                        selectedImageUri = getImageUriFromBitmap(selectedImageBitmap);
                    }
                }

                if (selectedImageBitmap != null) {
                    // תצוגה מיידית של התמונה שנבחרה
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

    // Helper method to convert bitmap to URI
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(
                requireContext().getContentResolver(), bitmap, "ProfileImage", null);
        return Uri.parse(path);
    }

    private void saveProfileChanges() {
        if (isUploading) {
            Toast.makeText(requireContext(), "Profile update already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקת שדות חובה
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

        // קבלת האימייל מהמשתמש המחובר
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";

        // יצירת אובייקט נתונים עם התמונה הנוכחית אם לא נבחרה חדשה
        Veterinarian profileData = new Veterinarian(
                fullName,
                clinicAddress,
                workHoursFirstPart,
                workHoursSecondPart,
                workHoursThirdPart,
                currentProfileImageUrl,
                email,
                phoneNumber,
                true, // isVet
                auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null
        );

        // סימון שתהליך הטעינה התחיל
        isUploading = true;

        // הצגת דיאלוג טעינה
        loadingDialog.setMessage(selectedImageBitmap != null ? "Uploading image..." : "Updating profile...");
        loadingDialog.show();

        // עדכון Firestore
        updateFirestoreData(profileData);
    }

    private void updateFirestoreData(Veterinarian profileData) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            isUploading = false;
            if (loadingDialog.isShowing()) loadingDialog.dismiss();
            return;
        }

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

        String vetId = auth.getCurrentUser().getUid();
        db.collection("Veterinarians").document(vetId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile data updated in Firestore successfully");

                    // שמירת שדות בסיסיים בSharedPreferences מיד
                    saveBasicDataToSharedPreferences(profileData);

                    // העלאת תמונה אם נבחרה - Use FirestoreUserHelper here
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

    private void saveBasicDataToSharedPreferences(Veterinarian profileData) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fullName", profileData.fullName);
        editor.putString("clinicAddress", profileData.clinicAddress);
        editor.putString("workHoursFirstPart", profileData.workHoursFirstPart);
        editor.putString("workHoursSecondPart", profileData.workHoursSecondPart);
        editor.putString("workHoursThirdPart", profileData.workHoursThirdPart);
        editor.putString("email", profileData.email);
        editor.putString("phoneNumber", profileData.phoneNumber);

        // שמירת ה-JSON המלא גם כן
        Gson gson = new Gson();
        String profileJson = gson.toJson(profileData);
        editor.putString("vet_profile_json", profileJson);

        editor.apply();
        Log.d(TAG, "Basic profile data saved to SharedPreferences");
    }

    // New method using FirestoreUserHelper instead of direct implementation
    private void uploadProfileImageUsingHelper(Uri imageUri, Veterinarian profileData) {
        String vetId = auth.getCurrentUser().getUid();

        // Use FirestoreUserHelper's image upload method
        FirestoreUserHelper.uploadVetProfileImage(imageUri, vetId, new FirestoreUserHelper.OnImageUploadListener() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                Log.d(TAG, "Image uploaded successfully via FirestoreUserHelper. URL: " + imageUrl);

                // Update the profileData with the new URL
                profileData.profileImageUrl = imageUrl;
                currentProfileImageUrl = imageUrl;

                // Update SharedPreferences with the new URL
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("profileImageUrl", imageUrl);
                editor.apply();

                // Complete the profile update process
                finishProfileUpdate(profileData);
            }

            @Override
            public void onUploadFailed(Exception e) {
                Log.e(TAG, "Failed to upload image via FirestoreUserHelper", e);
                Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                // Complete the profile update anyway, but without the new image
                finishProfileUpdate(profileData);
            }
        });
    }

    private void finishProfileUpdate(Veterinarian profileData) {
        try {
            // שמירת כל הנתונים ב-JSON ובשדות נפרדים
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // שמירת שדה התמונה בנפרד (חשוב!)
            if (profileData.profileImageUrl != null) {
                editor.putString("profileImageUrl", profileData.profileImageUrl);
                Log.d(TAG, "Saving final image URL to SharedPreferences: " + profileData.profileImageUrl);
            }

            // שמירת האובייקט המלא כ-JSON
            Gson gson = new Gson();
            String profileJson = gson.toJson(profileData);
            editor.putString("vet_profile_json", profileJson);

            // הבטחת שמירה מיידית
            editor.commit(); // Using commit instead of apply for immediate write

            Log.d(TAG, "Profile data saved to SharedPreferences as JSON");

            // עדכון לוקאלי בVetHomeFragment אם קיים
            try {
                Fragment parentFragment = getParentFragment();
                if (parentFragment != null && parentFragment.getClass().getSimpleName().equals("VetHomeFragment")) {
                    // ניסיון לקרוא לשיטה updateProfileView() בVetHomeFragment אם קיימת
                    java.lang.reflect.Method updateMethod = parentFragment.getClass().getDeclaredMethod("updateProfileView");
                    updateMethod.setAccessible(true);
                    updateMethod.invoke(parentFragment);
                    Log.d(TAG, "Called updateProfileView() on VetHomeFragment");
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't update VetHomeFragment", e);
            }

            // שמור שוב את כל השדות בפיירסטור (כולל התמונה)
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
            String vetId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (vetId != null) {
                db.collection("Veterinarians").document(vetId)
                    .set(updates, SetOptions.merge());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving profile data", e);
        }

        // סיום התהליך
        isUploading = false;
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
        navigateToVetHome();
    }

    // פונקציה חדשה: חזרה ל-VetHomeFragment
    private void navigateToVetHome() {
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new com.example.vetcalls.vetFragment.VetHomeFragment())
            .commit();
    }
}