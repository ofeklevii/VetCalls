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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
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
            String imageUrl = getBestImageUrl(currentDogProfile.profileImageUrl);
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
        adapter = new DogProfileAdapter(requireContext(), new ArrayList<>(), this, 1);
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

            // Save full dog list to shared preferences
            saveDogsListToPreferences(dogList);

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

        DogProfile dog = new DogProfile();
        dog.dogId = dogId;
        dog.name = bundle.getString("updatedUserName");
        dog.age = bundle.getString("updatedDogAge");
        dog.bio = bundle.getString("updatedBio");
        dog.profileImageUrl = imageUrl;
        dog.race = bundle.getString("race");
        dog.birthday = bundle.getString("birthday");
        dog.weight = bundle.getString("weight");
        dog.allergies = bundle.getString("allergies");
        dog.vaccines = bundle.getString("vaccines");
        dog.ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dog.vetId = null;
        return dog;
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
        editor.putString("name", dog.name);
        editor.putString("age", dog.age);
        editor.putString("race", dog.race);
        editor.putString("bio", dog.bio);
        editor.putString("birthday", dog.birthday);
        editor.putString("weight", dog.weight);
        editor.putString("allergies", dog.allergies);
        editor.putString("vaccines", dog.vaccines);

        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            editor.putString("profileImageUrl", dog.profileImageUrl);
            editor.putString("imageUrl", dog.profileImageUrl);
        }

        editor.apply();
    }

    // שמירת רשימת הכלבים המלאה לזיכרון המקומי
    private void saveDogsListToPreferences(List<DogProfile> dogs) {
        try {
            // המרת הרשימה למחרוזת JSON
            Gson gson = new Gson();
            String dogsJson = gson.toJson(dogs);

            // שמירה בזיכרון המקומי
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("dogs_list_json", dogsJson);
            editor.apply();

            Log.d(TAG, "Dogs list saved to SharedPreferences: " + dogs.size() + " dogs");
        } catch (Exception e) {
            Log.e(TAG, "Error saving dogs list: " + e.getMessage());
        }
    }

    // טעינת רשימת הכלבים מהזיכרון המקומי
    private List<DogProfile> loadDogsListFromPreferences() {
        List<DogProfile> dogs = new ArrayList<>();

        try {
            // שליפת מחרוזת ה-JSON מהזיכרון המקומי
            String dogsJson = sharedPreferences.getString("dogs_list_json", "");

            if (dogsJson != null && !dogsJson.isEmpty()) {
                // המרת המחרוזת לרשימת כלבים
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<DogProfile>>(){}.getType();
                dogs = gson.fromJson(dogsJson, listType);

                Log.d(TAG, "Dogs list loaded from SharedPreferences: " + dogs.size() + " dogs");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading dogs list: " + e.getMessage());
        }

        return dogs;
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

                        // שמירת הרשימה המלאה לזיכרון המקומי
                    saveDogsListToPreferences(newDogList);

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

            // שמירת הרשימה המלאה לזיכרון המקומי
            saveDogsListToPreferences(newDogList);

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

        // ודא שהכלב הנוכחי הוא הראשון ברשימה
        if (currentDogProfile != null && !dogList.isEmpty() && !dogList.get(0).getId().equals(currentDogProfile.getId())) {
            for (int i = 1; i < dogList.size(); i++) {
                if (dogList.get(i).getId().equals(currentDogProfile.getId())) {
                    DogProfile temp = dogList.get(0);
                    dogList.set(0, dogList.get(i));
                    dogList.set(i, temp);
                    break;
                }
            }
        }

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
        // מציגים את כל הכלבים חוץ מהראשון (הנוכחי)
        if (dogList.size() <= 1) return new ArrayList<>();
        return new ArrayList<>(dogList.subList(1, dogList.size()));
    }

    // Update dog display in the top section
    private void updateDogDisplay(DogProfile dog) {
        if (dog == null) {
            clearTopProfileDisplay();
            return;
        }

        // Set name and age
        userName.setText(dog.name);
        dogAge.setText("Age: " + (dog.age != null && !dog.age.isEmpty() ? dog.age : "Unknown"));

        // Set bio
        String bioText = createBioText(dog);
        bioTextView.setText(bioText);

        // Load image
        String imageUrl = getBestImageUrl(dog.profileImageUrl);
        loadProfileImage(profilePic, imageUrl);
    }

    private String createBioText(DogProfile dog) {
        StringBuilder bio = new StringBuilder();

        addIfNotEmpty(bio, "Weight", dog.weight, " kg");
        addIfNotEmpty(bio, "Race", dog.race, "");
        addIfNotEmpty(bio, "Allergies", dog.allergies, "");
        addIfNotEmpty(bio, "Vaccines", dog.vaccines, "");

        String result = bio.toString().trim();
        if (result.isEmpty()) {
            if (dog.bio != null && !dog.bio.isEmpty()) {
                return dog.bio;
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
            String name = document.getString("name");
            String bio = document.getString("bio");
            String race = document.getString("race");
            String birthday = document.getString("birthday");
            String allergies = document.getString("allergies");
            String vaccines = document.getString("vaccines");
            String ownerId = document.getString("ownerId");
            String vetId = document.getString("vetId");

            // Extract age and weight (can be number or string)
            String age = extractStringOrNumber(document, "age", "Unknown");
            String weight = extractStringOrNumber(document, "weight", "");

            // Get image URL (try both fields)
            String imageUrl = document.getString("profileImageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = document.getString("imageUrl");
            }
            imageUrl = getBestImageUrl(imageUrl);

            DogProfile dog = new DogProfile();
            dog.dogId = id;
            dog.name = name;
            dog.age = age;
            dog.bio = bio;
            dog.profileImageUrl = imageUrl;
            dog.race = race;
            dog.birthday = birthday;
            dog.weight = weight;
            dog.allergies = allergies;
            dog.vaccines = vaccines;
            dog.ownerId = ownerId;
            dog.vetId = vetId;
            return dog;
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
    public void onDogClick(int realIndex) {
        if (dogList.size() < 2) return;
        if (realIndex == 0) return; // לא מחליפים עם הראשי

        DogProfile temp = dogList.get(0);
        dogList.set(0, dogList.get(realIndex));
        dogList.set(realIndex, temp);

            currentDogProfile = dogList.get(0);
        currentDogProfile.setCurrent(true);
        dogList.get(realIndex).setCurrent(false);

            saveDogToPreferences(currentDogProfile);
        organizeDogsAndUpdateUI();
    }

    // Handle resume event
    @Override
    public void onResume() {
        super.onResume();

        // עדכון הממשק מנתונים מקומיים
        updateFromPreferences();

        // טעינת רשימת הכלבים המלאה מהזיכרון המקומי (תציג מיד)
        List<DogProfile> savedDogs = loadDogsListFromPreferences();
        if (!savedDogs.isEmpty()) {
            Log.d(TAG, "Loaded " + savedDogs.size() + " dogs from SharedPreferences");
            dogList.clear();
            dogList.addAll(savedDogs);
            organizeDogsAndUpdateUI();
        }

        // טעינה מהשרת ברקע
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