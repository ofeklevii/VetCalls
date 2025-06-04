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

public class VetHomeFragment extends Fragment {
    private ImageView vetProfileImage;
    private TextView vetFullName, vetSpecialty, vetEmail, vetClinicAddress, vetWorkHours, vetPhoneNumber;
    private Button editProfileButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences sharedPreferences;

    private static final String TAG = "VetHomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_home, container, false);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("VetProfile", Context.MODE_PRIVATE);

        // אתחול רכיבי View
        vetProfileImage = view.findViewById(R.id.vetProfileImage);
        vetFullName = view.findViewById(R.id.vetFullName);
        vetSpecialty = view.findViewById(R.id.vetSpecialty);
        vetEmail = view.findViewById(R.id.vetEmail);
        vetClinicAddress = view.findViewById(R.id.vetClinicAddress);
        vetWorkHours = view.findViewById(R.id.vetWorkHours);
        vetPhoneNumber = view.findViewById(R.id.vetPhoneNumber);
        editProfileButton = view.findViewById(R.id.editProfileButton);

        // הגדרת מאזין ללחיצה על כפתור עריכה
        editProfileButton.setOnClickListener(v -> openEditProfileFragment());

        loadVetProfileFromSharedPreferences();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // עדכון מיידי של הפרופיל בכל פעם שחוזרים לפרגמנט
        updateProfileView();
    }

    /**
     * שיטה חדשה - מעדכנת את תצוגת הפרופיל מיד
     * שיטה זו נקראת מ-EditVetProfileFragment כאשר משתמש שומר שינויים
     */
    public void updateProfileView() {
        Log.d(TAG, "updateProfileView() called - updating profile view");

        // קודם טען מהזיכרון המקומי (מהיר)
        loadVetProfileFromSharedPreferences();

        // אח"כ עדכן מהשרת (יכול להיות איטי יותר)
        loadVetProfileFromServer();
    }

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
                        Map<String, Object> vetMap = documentSnapshot.getData();
                        if (vetMap != null) {
                            // עדכון מספר טלפון
                            String phoneNumber = extractStringOrNumber(documentSnapshot, "phoneNumber", "");
                            vetMap.put("phoneNumber", phoneNumber);
                            // שמירה בזיכרון המקומי
                            saveVetProfileToSharedPreferences(vetMap);
                            // עדכון ממשק המשתמש
                            updateUIWithProfileData(vetMap);
                        }
                    } else {
                        Log.d(TAG, "No Firestore document exists for this vet");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load vet profile from Firestore", e);
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

    private void updateUIWithProfileData(Map<String, Object> profileData) {
        if (profileData == null) {
            Log.e(TAG, "Cannot update UI with null profile data");
            return;
        }
        Log.d(TAG, "updateUIWithProfileData: " + profileData);

        // עדכון שם מלא
        String fullName = profileData.get("fullName") != null ? profileData.get("fullName").toString() : "Veterinarian";
        vetFullName.setText(fullName);
        Log.d(TAG, "Setting vetFullName: " + fullName);

        // עדכון אימייל
        String email = profileData.get("email") != null ? profileData.get("email").toString() : null;
        if (email != null && !email.isEmpty()) {
            vetEmail.setText(email);
            Log.d(TAG, "Setting vetEmail: " + email);
        } else if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
            vetEmail.setText(auth.getCurrentUser().getEmail());
            Log.d(TAG, "Setting vetEmail (from auth): " + auth.getCurrentUser().getEmail());
        }

        // עדכון כתובת מרפאה
        String clinicAddress = profileData.get("clinicAddress") != null ? profileData.get("clinicAddress").toString() : "";
        vetClinicAddress.setText(clinicAddress);
        Log.d(TAG, "Setting vetClinicAddress: " + clinicAddress);

        // עדכון שעות קבלה
        String workHoursFirstPart = profileData.get("workHoursFirstPart") != null ? profileData.get("workHoursFirstPart").toString() : "Sunday - Thursday: 08:00 - 00:00";
        String workHoursSecondPart = profileData.get("workHoursSecondPart") != null ? profileData.get("workHoursSecondPart").toString() : "Friday: 08:00 - 16:00";
        String workHoursThirdPart = profileData.get("workHoursThirdPart") != null ? profileData.get("workHoursThirdPart").toString() : "Saturday: 19:00 - 23:00";
        String workHoursText = String.format("%s\n%s\n%s", workHoursFirstPart, workHoursSecondPart, workHoursThirdPart);
        vetWorkHours.setText(workHoursText);
        Log.d(TAG, "Setting vetWorkHours: " + workHoursText);

        // עדכון מספר טלפון
        String phoneNumber = profileData.get("phoneNumber") != null ? profileData.get("phoneNumber").toString() : "";
        vetPhoneNumber.setText(phoneNumber);
        Log.d(TAG, "Setting vetPhoneNumber: " + phoneNumber);

        // טעינת תמונת פרופיל עם שיפורים
        String profileImageUrl = profileData.get("profileImageUrl") != null ? profileData.get("profileImageUrl").toString() : null;
        Log.d(TAG, "Setting profileImageUrl: " + profileImageUrl);
        loadProfileImage(profileImageUrl);
    }

    // שיטה משופרת לטעינת תמונת פרופיל
    private void loadProfileImage(String imageUrl) {
        String bestImageUrl = getBestImageUrl(imageUrl);

        Log.d(TAG, "Loading profile image with URL: " + bestImageUrl);

        if (bestImageUrl != null && !bestImageUrl.isEmpty()) {
            try {
                // חשוב! שימוש ב-this במקום requireContext() למניעת בעיות כאשר הפרגמנט כבר לא מחובר
                Glide.with(this)
                        .load(bestImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // שמירה במטמון תמיד
                        .skipMemoryCache(false) // שימוש במטמון זיכרון
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

    // שיטה לקבלת כתובת התמונה הטובה ביותר
    private String getBestImageUrl(String proposedUrl) {
        if (proposedUrl != null && !proposedUrl.isEmpty()) {
            return proposedUrl;
        }

        // אם אין URL מוצע, נסה לקבל מהזיכרון המקומי
        String cachedUrl = sharedPreferences.getString("profileImageUrl", null);
        Log.d(TAG, "Using cached image URL from SharedPreferences: " + cachedUrl);
        return cachedUrl;
    }

    // שמירת נתוני הפרופיל בזיכרון המקומי
    private void saveVetProfileToSharedPreferences(Map<String, Object> profileData) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (profileData.get("fullName") != null) editor.putString("fullName", profileData.get("fullName").toString());
            if (profileData.get("clinicAddress") != null) editor.putString("clinicAddress", profileData.get("clinicAddress").toString());
            if (profileData.get("workHoursFirstPart") != null) editor.putString("workHoursFirstPart", profileData.get("workHoursFirstPart").toString());
            if (profileData.get("workHoursSecondPart") != null) editor.putString("workHoursSecondPart", profileData.get("workHoursSecondPart").toString());
            if (profileData.get("workHoursThirdPart") != null) editor.putString("workHoursThirdPart", profileData.get("workHoursThirdPart").toString());
            if (profileData.get("email") != null) editor.putString("email", profileData.get("email").toString());
            if (profileData.get("phoneNumber") != null) editor.putString("phoneNumber", profileData.get("phoneNumber").toString());
            editor.putString("profileImageUrl", profileData.get("profileImageUrl") != null ? profileData.get("profileImageUrl").toString() : null);
            Log.d(TAG, "Saving profile image URL to SharedPreferences: " + profileData.get("profileImageUrl"));
            // שמירת אובייקט הנתונים המלא כ-JSON
            Gson gson = new Gson();
            String profileJson = gson.toJson(profileData);
            editor.putString("vet_profile_json", profileJson);
            editor.commit();
            Log.d(TAG, "Vet profile saved to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving vet profile to SharedPreferences", e);
        }
    }

    // טעינת נתוני הפרופיל מהזיכרון המקומי
    private void loadVetProfileFromSharedPreferences() {
        try {
            Log.d(TAG, "Loading vet profile from SharedPreferences");

            // ניסיון טעינה מ-JSON תחילה (מהיר יותר)
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
                return;
            }

            // אם אין JSON, או שהוא שגוי, טען מהשדות הבודדים
            Log.d(TAG, "No valid JSON found, loading from individual fields");
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
            String directImageUrl = sharedPreferences.getString("profileImageUrl", null);
            if (directImageUrl != null && !directImageUrl.equals(profileImageUrl)) {
                profileDataMap.put("profileImageUrl", directImageUrl);
            }
            updateUIWithProfileData(profileDataMap);
            Log.d(TAG, "Loaded vet profile from individual fields in SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error loading vet profile from SharedPreferences", e);

            // במקרה של שגיאה, נסה להציג ערכי ברירת מחדל
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
    }

    private void openEditProfileFragment() {
        EditVetProfileFragment editFragment = new EditVetProfileFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }
}