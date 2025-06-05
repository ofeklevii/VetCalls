package com.example.vetcalls.vetFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.example.vetcalls.obj.Veterinarian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment displaying the veterinarian's home screen with profile information and edit functionality.
 * Manages profile data loading from both local storage and Firestore, with automatic synchronization.
 * Provides navigation to profile editing and handles image loading with caching.
 *
 * @author Ofek Levi
 */
public class VetHomeFragment extends Fragment {

    /** Tag for logging purposes */
    private static final String TAG = "VetHomeFragment";

    /** ImageView for displaying the veterinarian's profile picture */
    private ImageView vetProfileImage;

    /** TextView components for displaying veterinarian profile information */
    private TextView vetFullName, vetSpecialty, vetEmail, vetClinicAddress, vetWorkHours, vetPhoneNumber;

    /** Button for navigating to profile editing screen */
    private Button editProfileButton;

    /** Firebase Firestore database instance */
    private FirebaseFirestore db;

    /** Firebase authentication instance */
    private FirebaseAuth auth;

    /** SharedPreferences for local data storage */
    private SharedPreferences sharedPreferences;

    /**
     * Creates and initializes the fragment view with all UI components and data loading.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment's UI will be attached to
     * @param savedInstanceState Bundle containing the fragment's previously saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_home, container, false);

        initializeFirebase();
        initializeUIComponents(view);
        setupEditButton();
        loadVetProfileFromSharedPreferences();

        return view;
    }

    /**
     * Called when the fragment becomes visible to the user again.
     * Triggers an immediate profile update to ensure data freshness.
     */
    @Override
    public void onResume() {
        super.onResume();
        updateProfileView();
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
     * Initializes all UI components from the layout.
     *
     * @param view The root view of the fragment
     */
    private void initializeUIComponents(View view) {
        vetProfileImage = view.findViewById(R.id.vetProfileImage);
        vetFullName = view.findViewById(R.id.vetFullName);
        vetSpecialty = view.findViewById(R.id.vetSpecialty);
        vetEmail = view.findViewById(R.id.vetEmail);
        vetClinicAddress = view.findViewById(R.id.vetClinicAddress);
        vetWorkHours = view.findViewById(R.id.vetWorkHours);
        vetPhoneNumber = view.findViewById(R.id.vetPhoneNumber);
        editProfileButton = view.findViewById(R.id.editProfileButton);
    }

    /**
     * Sets up the edit profile button with click listener.
     */
    private void setupEditButton() {
        editProfileButton.setOnClickListener(v -> openEditProfileFragment());
    }

    /**
     * Updates the profile view immediately by loading from local storage first, then from server.
     * This method is called from EditVetProfileFragment when user saves changes.
     */
    public void updateProfileView() {
        Log.d(TAG, "updateProfileView() called - updating profile view");

        loadVetProfileFromSharedPreferences();
        loadVetProfileFromServer();
    }

    /**
     * Loads veterinarian profile data from Firestore server and updates local storage.
     * Handles authentication validation and error cases gracefully.
     */
    private void loadVetProfileFromServer() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User is not authenticated");
            return;
        }

        String vetId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail();
        Log.d(TAG, "Requesting profile data from Firestore for vet ID: " + vetId);

        db.collection("Veterinarians").document(vetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Firestore document exists, processing data");
                        processFirestoreDocument(documentSnapshot);
                    } else {
                        Log.d(TAG, "No Firestore document exists for this vet");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load vet profile from Firestore", e);
                });
    }

    /**
     * Processes Firestore document and updates UI with retrieved data.
     *
     * @param documentSnapshot The Firestore document containing veterinarian data
     */
    private void processFirestoreDocument(DocumentSnapshot documentSnapshot) {
        Map<String, Object> vetMap = documentSnapshot.getData();
        if (vetMap != null) {
            String phoneNumber = extractStringOrNumber(documentSnapshot, "phoneNumber", "");
            vetMap.put("phoneNumber", phoneNumber);
            saveVetProfileToSharedPreferences(vetMap);
            updateUIWithProfileData(vetMap);
        }
    }

    /**
     * Safely extracts string or number values from Firestore document.
     *
     * @param document The Firestore document
     * @param field The field name to extract
     * @param defaultValue The default value if field is not found or invalid
     * @return The extracted value as string, or default value
     */
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

    /**
     * Updates all UI components with the provided profile data.
     *
     * @param profileData Map containing veterinarian profile information
     */
    private void updateUIWithProfileData(Map<String, Object> profileData) {
        if (profileData == null) {
            Log.e(TAG, "Cannot update UI with null profile data");
            return;
        }
        Log.d(TAG, "updateUIWithProfileData: " + profileData);

        updateFullName(profileData);
        updateEmail(profileData);
        updateClinicAddress(profileData);
        updateWorkHours(profileData);
        updatePhoneNumber(profileData);
        updateProfileImage(profileData);
    }

    /**
     * Updates the full name display.
     *
     * @param profileData Map containing profile information
     */
    private void updateFullName(Map<String, Object> profileData) {
        String fullName = profileData.get("fullName") != null ? profileData.get("fullName").toString() : "Veterinarian";
        vetFullName.setText(fullName);
        Log.d(TAG, "Setting vetFullName: " + fullName);
    }

    /**
     * Updates the email display with fallback to authenticated user email.
     *
     * @param profileData Map containing profile information
     */
    private void updateEmail(Map<String, Object> profileData) {
        String email = profileData.get("email") != null ? profileData.get("email").toString() : null;
        if (email != null && !email.isEmpty()) {
            vetEmail.setText(email);
            Log.d(TAG, "Setting vetEmail: " + email);
        } else if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
            vetEmail.setText(auth.getCurrentUser().getEmail());
            Log.d(TAG, "Setting vetEmail (from auth): " + auth.getCurrentUser().getEmail());
        }
    }

    /**
     * Updates the clinic address display.
     *
     * @param profileData Map containing profile information
     */
    private void updateClinicAddress(Map<String, Object> profileData) {
        String clinicAddress = profileData.get("clinicAddress") != null ? profileData.get("clinicAddress").toString() : "";
        vetClinicAddress.setText(clinicAddress);
        Log.d(TAG, "Setting vetClinicAddress: " + clinicAddress);
    }

    /**
     * Updates the work hours display by combining three time period parts.
     *
     * @param profileData Map containing profile information
     */
    private void updateWorkHours(Map<String, Object> profileData) {
        String workHoursFirstPart = profileData.get("workHoursFirstPart") != null ? profileData.get("workHoursFirstPart").toString() : "Sunday - Thursday: 08:00 - 00:00";
        String workHoursSecondPart = profileData.get("workHoursSecondPart") != null ? profileData.get("workHoursSecondPart").toString() : "Friday: 08:00 - 16:00";
        String workHoursThirdPart = profileData.get("workHoursThirdPart") != null ? profileData.get("workHoursThirdPart").toString() : "Saturday: 19:00 - 23:00";
        String workHoursText = String.format("%s\n%s\n%s", workHoursFirstPart, workHoursSecondPart, workHoursThirdPart);
        vetWorkHours.setText(workHoursText);
        Log.d(TAG, "Setting vetWorkHours: " + workHoursText);
    }

    /**
     * Updates the phone number display.
     *
     * @param profileData Map containing profile information
     */
    private void updatePhoneNumber(Map<String, Object> profileData) {
        String phoneNumber = profileData.get("phoneNumber") != null ? profileData.get("phoneNumber").toString() : "";
        vetPhoneNumber.setText(phoneNumber);
        Log.d(TAG, "Setting vetPhoneNumber: " + phoneNumber);
    }

    /**
     * Updates the profile image display.
     *
     * @param profileData Map containing profile information
     */
    private void updateProfileImage(Map<String, Object> profileData) {
        String profileImageUrl = profileData.get("profileImageUrl") != null ? profileData.get("profileImageUrl").toString() : null;
        Log.d(TAG, "Setting profileImageUrl: " + profileImageUrl);
        loadProfileImage(profileImageUrl);
    }

    /**
     * Loads profile image using Glide with caching and error handling.
     *
     * @param imageUrl The URL of the image to load
     */
    private void loadProfileImage(String imageUrl) {
        String bestImageUrl = getBestImageUrl(imageUrl);

        Log.d(TAG, "Loading profile image with URL: " + bestImageUrl);

        if (bestImageUrl != null && !bestImageUrl.isEmpty()) {
            try {
                Glide.with(this)
                        .load(bestImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                        .error(R.drawable.user_person_profile_avatar_icon_190943)
                        .circleCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Failed to load image: " + bestImageUrl + ", error: " + (e != null ? e.getMessage() : "unknown error"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d(TAG, "Image loaded successfully from " + dataSource.name() + ", URL: " + bestImageUrl);
                                return false;
                            }
                        })
                        .into(vetProfileImage);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image with Glide", e);
                vetProfileImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
            }
        } else {
            Log.d(TAG, "No valid image URL, using default image");
            vetProfileImage.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }
    }

    /**
     * Gets the best available image URL from proposed URL or cached storage.
     *
     * @param proposedUrl The proposed image URL to use
     * @return The best available image URL or null if none found
     */
    private String getBestImageUrl(String proposedUrl) {
        if (proposedUrl != null && !proposedUrl.isEmpty()) {
            return proposedUrl;
        }

        String cachedUrl = sharedPreferences.getString("profileImageUrl", null);
        Log.d(TAG, "Using cached image URL from SharedPreferences: " + cachedUrl);
        return cachedUrl;
    }

    /**
     * Saves veterinarian profile data to SharedPreferences for local caching.
     *
     * @param profileData Map containing profile information to save
     */
    private void saveVetProfileToSharedPreferences(Map<String, Object> profileData) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            saveIndividualFields(editor, profileData);
            saveCompleteProfileAsJson(editor, profileData);

            editor.commit();
            Log.d(TAG, "Vet profile saved to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving vet profile to SharedPreferences", e);
        }
    }

    /**
     * Saves individual profile fields to SharedPreferences.
     *
     * @param editor SharedPreferences editor
     * @param profileData Map containing profile information
     */
    private void saveIndividualFields(SharedPreferences.Editor editor, Map<String, Object> profileData) {
        if (profileData.get("fullName") != null) editor.putString("fullName", profileData.get("fullName").toString());
        if (profileData.get("clinicAddress") != null) editor.putString("clinicAddress", profileData.get("clinicAddress").toString());
        if (profileData.get("workHoursFirstPart") != null) editor.putString("workHoursFirstPart", profileData.get("workHoursFirstPart").toString());
        if (profileData.get("workHoursSecondPart") != null) editor.putString("workHoursSecondPart", profileData.get("workHoursSecondPart").toString());
        if (profileData.get("workHoursThirdPart") != null) editor.putString("workHoursThirdPart", profileData.get("workHoursThirdPart").toString());
        if (profileData.get("email") != null) editor.putString("email", profileData.get("email").toString());
        if (profileData.get("phoneNumber") != null) editor.putString("phoneNumber", profileData.get("phoneNumber").toString());
        editor.putString("profileImageUrl", profileData.get("profileImageUrl") != null ? profileData.get("profileImageUrl").toString() : null);
        Log.d(TAG, "Saving profile image URL to SharedPreferences: " + profileData.get("profileImageUrl"));
    }

    /**
     * Saves the complete profile data as JSON to SharedPreferences.
     *
     * @param editor SharedPreferences editor
     * @param profileData Map containing profile information
     */
    private void saveCompleteProfileAsJson(SharedPreferences.Editor editor, Map<String, Object> profileData) {
        Gson gson = new Gson();
        String profileJson = gson.toJson(profileData);
        editor.putString("vet_profile_json", profileJson);
    }

    /**
     * Loads veterinarian profile data from SharedPreferences.
     * Attempts to load from JSON first, falls back to individual fields if needed.
     */
    private void loadVetProfileFromSharedPreferences() {
        try {
            Log.d(TAG, "Loading vet profile from SharedPreferences");

            if (loadFromJsonPreferences()) {
                return;
            }

            loadFromIndividualFields();
        } catch (Exception e) {
            Log.e(TAG, "Error loading vet profile from SharedPreferences", e);
            loadDefaultProfileData();
        }
    }

    /**
     * Attempts to load profile data from JSON stored in SharedPreferences.
     *
     * @return true if successful, false if JSON not found or invalid
     */
    private boolean loadFromJsonPreferences() {
        String profileJson = sharedPreferences.getString("vet_profile_json", null);
        if (profileJson != null && !profileJson.isEmpty()) {
            Gson gson = new Gson();
            Map<String, Object> profileDataMap = gson.fromJson(profileJson, Map.class);

            String directImageUrl = sharedPreferences.getString("profileImageUrl", null);
            if (directImageUrl != null && !directImageUrl.equals(profileDataMap.get("profileImageUrl"))) {
                profileDataMap.put("profileImageUrl", directImageUrl);
            }

            updateUIWithProfileData(profileDataMap);
            Log.d(TAG, "Loaded vet profile from JSON in SharedPreferences");
            return true;
        }
        return false;
    }

    /**
     * Loads profile data from individual SharedPreferences fields.
     */
    private void loadFromIndividualFields() {
        Log.d(TAG, "No valid JSON found, loading from individual fields");

        Map<String, Object> profileDataMap = createProfileMapFromIndividualFields();
        String directImageUrl = sharedPreferences.getString("profileImageUrl", null);
        String existingImageUrl = (String) profileDataMap.get("profileImageUrl");

        if (directImageUrl != null && !directImageUrl.equals(existingImageUrl)) {
            profileDataMap.put("profileImageUrl", directImageUrl);
        }

        updateUIWithProfileData(profileDataMap);
        Log.d(TAG, "Loaded vet profile from individual fields in SharedPreferences");
    }

    /**
     * Creates a profile data map from individual SharedPreferences fields.
     *
     * @return Map containing profile data from individual fields
     */
    private Map<String, Object> createProfileMapFromIndividualFields() {
        String fullName = sharedPreferences.getString("fullName", "Veterinarian");
        String clinicAddress = sharedPreferences.getString("clinicAddress", "");
        String workHoursFirstPart = sharedPreferences.getString("workHoursFirstPart", "Sunday - Thursday: 08:00 - 00:00");
        String workHoursSecondPart = sharedPreferences.getString("workHoursSecondPart", "Friday: 08:00 - 16:00");
        String workHoursThirdPart = sharedPreferences.getString("workHoursThirdPart", "Saturday: 19:00 - 23:00");
        String profileImageUrl = sharedPreferences.getString("profileImageUrl", null);
        String email = sharedPreferences.getString("email", null);
        String phoneNumber = sharedPreferences.getString("phoneNumber", "");

        if (email == null && auth.getCurrentUser() != null) {
            email = auth.getCurrentUser().getEmail();
        }

        Map<String, Object> profileDataMap = new HashMap<>();
        profileDataMap.put("fullName", fullName);
        profileDataMap.put("clinicAddress", clinicAddress);
        profileDataMap.put("workHoursFirstPart", workHoursFirstPart);
        profileDataMap.put("workHoursSecondPart", workHoursSecondPart);
        profileDataMap.put("workHoursThirdPart", workHoursThirdPart);
        profileDataMap.put("profileImageUrl", profileImageUrl);
        profileDataMap.put("email", email);
        profileDataMap.put("phoneNumber", phoneNumber);

        return profileDataMap;
    }

    /**
     * Loads default profile data when SharedPreferences loading fails.
     */
    private void loadDefaultProfileData() {
        Map<String, Object> defaultDataMap = new HashMap<>();
        defaultDataMap.put("fullName", "Veterinarian");
        defaultDataMap.put("clinicAddress", "");
        defaultDataMap.put("workHoursFirstPart", "Sunday - Thursday: 08:00 - 00:00");
        defaultDataMap.put("workHoursSecondPart", "Friday: 08:00 - 16:00");
        defaultDataMap.put("workHoursThirdPart", "Saturday: 19:00 - 23:00");
        defaultDataMap.put("profileImageUrl", null);
        defaultDataMap.put("email", auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");
        defaultDataMap.put("phoneNumber", "");
        updateUIWithProfileData(defaultDataMap);
    }

    /**
     * Opens the edit profile fragment for veterinarian profile modification.
     */
    private void openEditProfileFragment() {
        EditVetProfileFragment editFragment = new EditVetProfileFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }
}