package com.example.vetcalls.usersFragment;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.vetcalls.R;
import com.example.vetcalls.activities.LoginActivity;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.DogProfileAdapter;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment implements DogProfileAdapter.OnDogClickListener {

    private static final String TAG = "HomeFragment";
    private TextView bioTextView, dogAge, userName;
    private ImageView profilePic;
    private RecyclerView dogRecyclerView;
    private DogProfileAdapter adapter;
    private List<DogProfile> dogList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private DogProfile currentDogProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeUiComponents(view);
        setupButtons(view);
        setupRecyclerView(view);
        setupFragmentResultListener();
        return view;
    }

    private void initializeUiComponents(View view) {
        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        profilePic = view.findViewById(R.id.profilePic);
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    private void setupButtons(View view) {
        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        Button addDogButton = view.findViewById(R.id.addDogButton);
        Button deleteAccountButton = view.findViewById(R.id.deleteAccountButton);

        // Set up Edit Profile button
        editProfileButton.setOnClickListener(v -> {
            Bundle args = createEditProfileArgs();
            launchEditProfileFragment(args);
        });

        // Set up Add Dog button
        addDogButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("dogId", ""); // Empty string indicates new dog
            launchEditProfileFragment(args);
        });

        // Set up Delete Account button
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private Bundle createEditProfileArgs() {
        Bundle args = new Bundle();

        if (currentDogProfile != null && currentDogProfile.getId() != null) {
            args.putString("dogId", currentDogProfile.getId());

            // העברת כתובת התמונה לפרגמנט העריכה
            String imageUrl = getBestImageUrl(currentDogProfile.getImageUrl());
            if (imageUrl != null && !imageUrl.isEmpty()) {
                args.putString("imageUrl", imageUrl);
            }

            Log.d(TAG, "Launching edit with dog: " + currentDogProfile.getId() +
                    ", image URL: " + args.getString("imageUrl", "none"));
        } else {
            String savedDogId = sharedPreferences.getString("dogId", null);
            if (savedDogId != null) {
                args.putString("dogId", savedDogId);

                // העברת כתובת התמונה גם כאן
                String imageUrl = getBestImageUrl(null);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    args.putString("imageUrl", imageUrl);
                }

                Log.d(TAG, "Launching edit with saved dog: " + savedDogId +
                        ", image URL: " + args.getString("imageUrl", "none"));
            } else {
                Log.d(TAG, "Launching edit with new dog (no ID)");
            }
        }

        return args;
    }

    private void launchEditProfileFragment(Bundle args) {
        EditProfileFragment editFragment = new EditProfileFragment();
        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupRecyclerView(View view) {
        dogRecyclerView = view.findViewById(R.id.dogRecyclerView);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DogProfileAdapter(requireContext(), dogList, this);
        dogRecyclerView.setAdapter(adapter);
    }

    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener("editProfileKey", this, (requestKey, bundle) -> {
            String dogId = bundle.getString("dogId");
            if (dogId == null) return;

            // Create updated dog profile from bundle data
            DogProfile updatedDog = createDogFromBundle(bundle);

            // Update current dog profile
            currentDogProfile = updatedDog;
            currentDogProfile.setCurrent(true);

            // Update dog in list or add if new
            updateDogInList(updatedDog);

            // Save to SharedPreferences and update UI
            saveDogToPreferences(updatedDog);
            organizeDogsAndUpdateUI();

            // Refresh from server
            loadAllDogProfilesFromFirestore();
        });
    }

    private DogProfile createDogFromBundle(Bundle bundle) {
        String dogId = bundle.getString("dogId");
        String imageUrl = bundle.getString("updatedImageUri");

        // If no image URL provided, try to get from preferences
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = getBestImageUrl(null);
        }

        return new DogProfile(
                dogId,
                bundle.getString("updatedUserName"),
                bundle.getString("updatedDogAge"),
                bundle.getString("updatedBio"),
                imageUrl,
                bundle.getString("race"),
                bundle.getString("birthday"),
                bundle.getString("weight"),
                bundle.getString("allergies"),
                bundle.getString("vaccines"),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                null
        );
    }

    private void updateDogInList(DogProfile newDog) {
        boolean dogExists = false;
        for (int i = 0; i < dogList.size(); i++) {
            if (dogList.get(i).getId() != null && dogList.get(i).getId().equals(newDog.getId())) {
                dogList.set(i, newDog);
                dogExists = true;
                break;
            }
        }

        if (!dogExists) {
            dogList.add(newDog);
        }
    }

    private void saveDogToPreferences(DogProfile dog) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("dogId", dog.getId());
        editor.putString("name", dog.getName());
        editor.putString("age", dog.getAge());
        editor.putString("race", dog.getRace());
        editor.putString("bio", dog.getBio());
        editor.putString("birthday", dog.getBirthday());
        editor.putString("weight", dog.getWeight());
        editor.putString("allergies", dog.getAllergies());
        editor.putString("vaccines", dog.getVaccines());

        if (dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
            editor.putString("profileImageUrl", dog.getImageUrl());
            editor.putString("imageUrl", dog.getImageUrl());
        }

        editor.apply();
    }

    // Show confirmation dialog for account deletion
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("מחיקת חשבון")
                .setMessage("האם אתה בטוח שברצונך למחוק את החשבון שלך? פעולה זו תמחק את כל הנתונים הקשורים לחשבון שלך ואינה ניתנת לביטול.")
                .setPositiveButton("כן, מחק", (dialog, which) -> {
                    AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                            .setMessage("מוחק חשבון...")
                            .setCancelable(false)
                            .create();
                    loadingDialog.show();

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        FirestoreUserHelper.deleteUserCompletely(currentUser.getUid(),
                                () -> {
                                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                                    Toast.makeText(requireContext(), "החשבון נמחק בהצלחה", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(requireContext(), LoginActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                    requireActivity().finish();
                                },
                                () -> {
                                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                                    Toast.makeText(requireContext(), "שגיאה במחיקת החשבון, נסה שוב מאוחר יותר", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(requireContext(), "אתה לא מחובר למערכת", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("לא, בטל", null)
                .show();
    }

    // Helper method to load profile image
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

    // Helper method to get best available image URL
    private String getBestImageUrl(String proposedUrl) {
        if (proposedUrl != null && !proposedUrl.isEmpty()) {
            return proposedUrl;
        }

        String savedUrl = sharedPreferences.getString("profileImageUrl", null);
        if (savedUrl == null || savedUrl.isEmpty()) {
            savedUrl = sharedPreferences.getString("imageUrl", null);
        }
        return savedUrl;
    }

    // Clear top profile display
    private void clearTopProfileDisplay() {
        userName.setText("Username");
        dogAge.setText("Age");
        bioTextView.setText("Your bio goes here...");
        profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        currentDogProfile = null;
    }

    // Load all dog profiles from Firestore
    private void loadAllDogProfilesFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null!");
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        dogList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    DocumentSnapshot firstDoc = queryDocumentSnapshots.getDocuments().get(0);
                    if (firstDoc.contains("dogId") && !firstDoc.contains("bio")) {
                        // Reference style - load dogs from DogProfiles collection
                        List<String> dogIds = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String dogId = doc.getString("dogId");
                            if (dogId != null) dogIds.add(dogId);
                        }

                        if (!dogIds.isEmpty()) {
                            loadDogsFromDogProfiles(dogIds);
                        } else {
                            dogList.clear();
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        // Old style - everything is in subcollection
                        List<DogProfile> newDogList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                DogProfile dogProfile = createDogProfileFromDocument(doc);
                                if (dogProfile != null) {
                                    newDogList.add(dogProfile);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing dog document: " + e.getMessage());
                            }
                        }

                        dogList.clear();
                        dogList.addAll(newDogList);
                        organizeDogsAndUpdateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog profiles: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dogs", Toast.LENGTH_SHORT).show();
                });
    }

    // Load dogs from DogProfiles collection
    private void loadDogsFromDogProfiles(List<String> dogIds) {
        List<DogProfile> newDogList = new ArrayList<>();
        Set<String> loadedDogIds = new HashSet<>();
        final int[] loadedCount = {0};
        final int totalCount = dogIds.size();

        for (String dogId : dogIds) {
            if (loadedDogIds.contains(dogId)) {
                incrementCounterAndCheckCompletion(loadedCount, totalCount, newDogList);
                continue;
            }

            loadedDogIds.add(dogId);
            FirebaseFirestore.getInstance()
                    .collection("DogProfiles")
                    .document(dogId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            try {
                                DogProfile dogProfile = createDogProfileFromDocument(document);
                                if (dogProfile != null && !isDuplicate(newDogList, dogProfile)) {
                                    markAsCurrentIfNeeded(dogProfile);
                                    newDogList.add(dogProfile);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating dog profile: " + e.getMessage());
                            }
                        }
                        incrementCounterAndCheckCompletion(loadedCount, totalCount, newDogList);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading dog profile: " + e.getMessage());
                        incrementCounterAndCheckCompletion(loadedCount, totalCount, newDogList);
                    });
        }
    }

    private void incrementCounterAndCheckCompletion(int[] loadedCount, int totalCount, List<DogProfile> newDogList) {
        loadedCount[0]++;
        if (loadedCount[0] >= totalCount) {
            dogList.clear();
            dogList.addAll(newDogList);
            organizeDogsAndUpdateUI();
        }
    }

    private boolean isDuplicate(List<DogProfile> dogList, DogProfile dog) {
        for (DogProfile existingDog : dogList) {
            if (existingDog.getId() != null && existingDog.getId().equals(dog.getId())) {
                return true;
            }
        }
        return false;
    }

    private void markAsCurrentIfNeeded(DogProfile dog) {
        String currentDogId = sharedPreferences.getString("dogId", null);
        if (currentDogId != null && currentDogId.equals(dog.getId())) {
            currentDogProfile = dog;
            dog.setCurrent(true);
        }
    }

    // Organize dogs and update UI
    private void organizeDogsAndUpdateUI() {
        if (dogList.isEmpty()) {
            clearTopProfileDisplay();
            adapter.notifyDataSetChanged();
            return;
        }

        ensureCurrentDogIsSet();
        List<DogProfile> filteredList = createFilteredList();

        if (currentDogProfile != null) {
            updateDogDisplay(currentDogProfile);
        }

        adapter.updateDogList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void ensureCurrentDogIsSet() {
        // Find current dog based on saved ID
        String savedDogId = sharedPreferences.getString("dogId", null);
        boolean foundCurrentDog = false;

        if (savedDogId != null && !savedDogId.isEmpty()) {
            for (DogProfile dog : dogList) {
                if (dog.getId() != null && dog.getId().equals(savedDogId)) {
                    currentDogProfile = dog;
                    dog.setCurrent(true);
                    foundCurrentDog = true;
                    break;
                }
            }
        }

        // If we didn't find the current dog, take the first one
        if (!foundCurrentDog && !dogList.isEmpty()) {
            currentDogProfile = dogList.get(0);
            currentDogProfile.setCurrent(true);
            saveDogToPreferences(currentDogProfile);
        }
    }

    private List<DogProfile> createFilteredList() {
        List<DogProfile> filteredList = new ArrayList<>();
        Set<String> addedDogIds = new HashSet<>();

        // Add current dog's ID to tracking so it won't be added to the list
        if (currentDogProfile != null && currentDogProfile.getId() != null) {
            addedDogIds.add(currentDogProfile.getId());
        }

        // Add all non-current dogs that aren't already in the list
        for (DogProfile dog : dogList) {
            if (dog.getId() == null || addedDogIds.contains(dog.getId())) continue;

            dog.setCurrent(false);
            filteredList.add(dog);
            addedDogIds.add(dog.getId());
        }

        return filteredList;
    }

    // Update dog display in the top section
    private void updateDogDisplay(DogProfile dog) {
        if (dog == null) {
            clearTopProfileDisplay();
            return;
        }

        // Set name and age
        userName.setText(dog.getName());
        dogAge.setText("Age: " + (dog.getAge() != null && !dog.getAge().isEmpty() ? dog.getAge() : "Unknown"));

        // Set bio
        String bioText = createBioText(dog);
        bioTextView.setText(bioText);

        // Load image
        String imageUrl = getBestImageUrl(dog.getImageUrl());
        loadProfileImage(profilePic, imageUrl);
    }

    private String createBioText(DogProfile dog) {
        StringBuilder bio = new StringBuilder();

        addIfNotEmpty(bio, "Weight", dog.getWeight(), " kg");
        addIfNotEmpty(bio, "Race", dog.getRace(), "");
        addIfNotEmpty(bio, "Allergies", dog.getAllergies(), "");
        addIfNotEmpty(bio, "Vaccines", dog.getVaccines(), "");

        String result = bio.toString().trim();
        if (result.isEmpty()) {
            if (dog.getBio() != null && !dog.getBio().isEmpty()) {
                return dog.getBio();
            } else {
                return "No information available";
            }
        }

        return result;
    }

    private void addIfNotEmpty(StringBuilder bio, String label, String value, String suffix) {
        if (value != null && !value.isEmpty()) {
            if (bio.length() > 0) bio.append("\n");
            bio.append(label).append(": ").append(value).append(suffix);
        }
    }

    // Create dog profile from document
    private DogProfile createDogProfileFromDocument(DocumentSnapshot document) {
        try {
            String id = document.getId();

            // Extract age (can be number or string)
            String age = extractStringOrNumber(document, "age", "Unknown");

            // Extract weight (can be number or string)
            String weight = extractStringOrNumber(document, "weight", "");

            // Get image URL (try both fields)
            String imageUrl = document.getString("profileImageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = document.getString("imageUrl");
            }

            // If still no image, check SharedPreferences
            imageUrl = getBestImageUrl(imageUrl);

            return new DogProfile(
                    id,
                    document.getString("name"),
                    age,
                    document.getString("bio"),
                    imageUrl,
                    document.getString("race"),
                    document.getString("birthday"),
                    weight,
                    document.getString("allergies"),
                    document.getString("vaccines"),
                    document.getString("ownerId"),
                    document.getString("vetId")
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating dog from document: " + e.getMessage());
            return null;
        }
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

    // Handle dog click in RecyclerView
    @Override
    public void onDogClick(DogProfile dogProfile) {
        if (dogProfile == null) return;

        saveDogToPreferences(dogProfile);
        currentDogProfile = dogProfile;
        updateDogDisplay(dogProfile);
        organizeDogsAndUpdateUI();
    }

    // Handle resume event
    @Override
    public void onResume() {
        super.onResume();
        updateFromPreferences();
        loadAllDogProfilesFromFirestore();
    }

    // Update UI from SharedPreferences
    private void updateFromPreferences() {
        // Update text fields from preferences
        String name = sharedPreferences.getString("name", null);
        String age = sharedPreferences.getString("age", null);
        String race = sharedPreferences.getString("race", null);
        String weight = sharedPreferences.getString("weight", null);
        String allergies = sharedPreferences.getString("allergies", null);
        String vaccines = sharedPreferences.getString("vaccines", null);
        String bio = sharedPreferences.getString("bio", null);

        if (name != null && !name.isEmpty()) {
            userName.setText(name);
        }

        if (age != null && !age.isEmpty()) {
            dogAge.setText("Age: " + age);
        }

        // Create and set bio text
        StringBuilder bioBuilder = new StringBuilder();
        addIfNotEmpty(bioBuilder, "Weight", weight, " kg");
        addIfNotEmpty(bioBuilder, "Race", race, "");
        addIfNotEmpty(bioBuilder, "Allergies", allergies, "");
        addIfNotEmpty(bioBuilder, "Vaccines", vaccines, "");

        String builtBio = bioBuilder.toString().trim();
        if (!builtBio.isEmpty()) {
            bioTextView.setText(builtBio);
        } else if (bio != null && !bio.isEmpty()) {
            bioTextView.setText(bio);
        }

        // Load image
        String imageUrl = getBestImageUrl(null);
        loadProfileImage(profilePic, imageUrl);
    }
}