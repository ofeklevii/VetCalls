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

/**
 * Main home fragment that displays the current dog's profile and manages multiple dog profiles.
 * Handles profile editing, dog switching, account deletion, and data synchronization with Firestore.
 *
 * @author Ofek Levi
 */
public class HomeFragment extends Fragment implements DogProfileAdapter.OnDogClickListener {

    /** Tag for logging purposes */
    private static final String TAG = "HomeFragment";

    /** TextView for displaying the current dog's bio information */
    private TextView bioTextView;

    /** TextView for displaying the current dog's age */
    private TextView dogAge;

    /** TextView for displaying the current dog's name */
    private TextView userName;

    /** ImageView for displaying the current dog's profile picture */
    private ImageView profilePic;

    /** RecyclerView for displaying the list of other dogs */
    private RecyclerView dogRecyclerView;

    /** Adapter for managing dog profiles in the RecyclerView */
    private DogProfileAdapter adapter;

    /** List containing all dog profiles for the current user */
    private List<DogProfile> dogList = new ArrayList<>();

    /** SharedPreferences instance for local data storage */
    private SharedPreferences sharedPreferences;

    /** Currently selected dog profile displayed in the main section */
    private DogProfile currentDogProfile;

    /**
     * Creates and initializes the fragment view with all UI components and listeners.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment's UI will be attached to
     * @param savedInstanceState Bundle containing the fragment's previously saved state
     * @return The View for the fragment's UI
     */
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

    /**
     * Initializes all UI components from the layout and SharedPreferences.
     *
     * @param view The root view of the fragment
     */
    private void initializeUiComponents(View view) {
        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        profilePic = view.findViewById(R.id.profilePic);
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    /**
     * Sets up click listeners for all buttons in the fragment.
     *
     * @param view The root view of the fragment
     */
    private void setupButtons(View view) {
        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        Button addDogButton = view.findViewById(R.id.addDogButton);
        Button deleteAccountButton = view.findViewById(R.id.deleteAccountButton);

        editProfileButton.setOnClickListener(v -> {
            Bundle args = createEditProfileArgs();
            launchEditProfileFragment(args);
        });

        addDogButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("dogId", "");
            launchEditProfileFragment(args);
        });

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    /**
     * Creates arguments bundle for launching the edit profile fragment.
     *
     * @return Bundle containing dog ID and image URL for editing
     */
    private Bundle createEditProfileArgs() {
        Bundle args = new Bundle();

        if (currentDogProfile != null && currentDogProfile.getId() != null) {
            args.putString("dogId", currentDogProfile.getId());

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

    /**
     * Launches the edit profile fragment with the provided arguments.
     *
     * @param args Bundle containing arguments for the edit profile fragment
     */
    private void launchEditProfileFragment(Bundle args) {
        EditProfileFragment editFragment = new EditProfileFragment();
        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Sets up the RecyclerView with layout manager and adapter for displaying dog profiles.
     *
     * @param view The root view of the fragment
     */
    private void setupRecyclerView(View view) {
        dogRecyclerView = view.findViewById(R.id.dogRecyclerView);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DogProfileAdapter(requireContext(), new ArrayList<>(), this, 1);
        dogRecyclerView.setAdapter(adapter);
    }

    /**
     * Sets up the fragment result listener to handle results from the edit profile fragment.
     * Updates the dog list and UI when profile editing is completed.
     */
    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener("editProfileKey", this, (requestKey, bundle) -> {
            String dogId = bundle.getString("dogId");
            if (dogId == null) return;

            DogProfile updatedDog = createDogFromBundle(bundle);

            currentDogProfile = updatedDog;
            currentDogProfile.setCurrent(true);

            updateDogInList(updatedDog);

            saveDogToPreferences(updatedDog);
            organizeDogsAndUpdateUI();

            saveDogsListToPreferences(dogList);

            loadAllDogProfilesFromFirestore();
        });
    }

    /**
     * Creates a DogProfile object from the provided bundle data.
     *
     * @param bundle Bundle containing dog profile data
     * @return DogProfile object created from bundle data
     */
    private DogProfile createDogFromBundle(Bundle bundle) {
        String dogId = bundle.getString("dogId");
        String imageUrl = bundle.getString("updatedImageUri");

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

    /**
     * Updates an existing dog in the list or adds a new one if it doesn't exist.
     *
     * @param newDog The dog profile to update or add
     */
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

    /**
     * Saves the provided dog profile data to SharedPreferences.
     *
     * @param dog The dog profile to save
     */
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

    /**
     * Saves the complete list of dogs to SharedPreferences as JSON.
     *
     * @param dogs List of dog profiles to save
     */
    private void saveDogsListToPreferences(List<DogProfile> dogs) {
        try {
            Gson gson = new Gson();
            String dogsJson = gson.toJson(dogs);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("dogs_list_json", dogsJson);
            editor.apply();

            Log.d(TAG, "Dogs list saved to SharedPreferences: " + dogs.size() + " dogs");
        } catch (Exception e) {
            Log.e(TAG, "Error saving dogs list: " + e.getMessage());
        }
    }

    /**
     * Loads the complete list of dogs from SharedPreferences JSON data.
     *
     * @return List of dog profiles loaded from preferences
     */
    private List<DogProfile> loadDogsListFromPreferences() {
        List<DogProfile> dogs = new ArrayList<>();

        try {
            String dogsJson = sharedPreferences.getString("dogs_list_json", "");

            if (dogsJson != null && !dogsJson.isEmpty()) {
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

    /**
     * Shows a confirmation dialog for account deletion with progress indicator.
     * Handles the complete account deletion process including navigation to login.
     */
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

    /**
     * Loads a profile image into the specified ImageView using Glide with caching and error handling.
     *
     * @param imageView The ImageView to load the image into
     * @param imageUrl The URL of the image to load
     */
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

    /**
     * Gets the best available image URL from proposed URL or SharedPreferences.
     *
     * @param proposedUrl The proposed image URL to use
     * @return The best available image URL or null if none found
     */
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

    /**
     * Clears the top profile display section and resets to default values.
     */
    private void clearTopProfileDisplay() {
        userName.setText("Username");
        dogAge.setText("Age");
        bioTextView.setText("Your bio goes here...");
        profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        currentDogProfile = null;
    }

    /**
     * Loads all dog profiles for the current user from Firestore.
     * Handles both reference-style and direct data storage in subcollections.
     */
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

                        saveDogsListToPreferences(newDogList);

                        organizeDogsAndUpdateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog profiles: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dogs", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads dog profiles from the DogProfiles collection using provided dog IDs.
     *
     * @param dogIds List of dog IDs to load profiles for
     */
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

    /**
     * Increments the counter and checks if all async operations are complete.
     *
     * @param loadedCount Array containing the current count of loaded dogs
     * @param totalCount Total number of dogs to load
     * @param newDogList List to store the loaded dog profiles
     */
    private void incrementCounterAndCheckCompletion(int[] loadedCount, int totalCount, List<DogProfile> newDogList) {
        loadedCount[0]++;
        if (loadedCount[0] >= totalCount) {
            dogList.clear();
            dogList.addAll(newDogList);

            saveDogsListToPreferences(newDogList);

            organizeDogsAndUpdateUI();
        }
    }

    /**
     * Checks if a dog profile is already in the list to prevent duplicates.
     *
     * @param dogList List of existing dog profiles
     * @param dog Dog profile to check for duplication
     * @return true if dog is duplicate, false otherwise
     */
    private boolean isDuplicate(List<DogProfile> dogList, DogProfile dog) {
        for (DogProfile existingDog : dogList) {
            if (existingDog.getId() != null && existingDog.getId().equals(dog.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marks a dog as current if it matches the saved dog ID in preferences.
     *
     * @param dog Dog profile to potentially mark as current
     */
    private void markAsCurrentIfNeeded(DogProfile dog) {
        String currentDogId = sharedPreferences.getString("dogId", null);
        if (currentDogId != null && currentDogId.equals(dog.getId())) {
            currentDogProfile = dog;
            dog.setCurrent(true);
        }
    }

    /**
     * Organizes the dog list and updates the UI display.
     * Ensures the current dog is first in the list and updates all UI components.
     */
    private void organizeDogsAndUpdateUI() {
        if (dogList.isEmpty()) {
            clearTopProfileDisplay();
            adapter.notifyDataSetChanged();
            return;
        }

        ensureCurrentDogIsSet();

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

    /**
     * Ensures that a current dog is set from the saved preferences or defaults to first dog.
     */
    private void ensureCurrentDogIsSet() {
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

        if (!foundCurrentDog && !dogList.isEmpty()) {
            currentDogProfile = dogList.get(0);
            currentDogProfile.setCurrent(true);
            saveDogToPreferences(currentDogProfile);
        }
    }

    /**
     * Creates a filtered list of dogs excluding the current dog.
     *
     * @return List of dog profiles excluding the first (current) dog
     */
    private List<DogProfile> createFilteredList() {
        if (dogList.size() <= 1) return new ArrayList<>();
        return new ArrayList<>(dogList.subList(1, dogList.size()));
    }

    /**
     * Updates the main dog display section with the provided dog's information.
     *
     * @param dog The dog profile to display
     */
    private void updateDogDisplay(DogProfile dog) {
        if (dog == null) {
            clearTopProfileDisplay();
            return;
        }

        userName.setText(dog.name);
        dogAge.setText("Age: " + (dog.age != null && !dog.age.isEmpty() ? dog.age : "Unknown"));

        String bioText = createBioText(dog);
        bioTextView.setText(bioText);

        String imageUrl = getBestImageUrl(dog.profileImageUrl);
        loadProfileImage(profilePic, imageUrl);
    }

    /**
     * Creates a formatted bio text from the dog's information.
     *
     * @param dog The dog profile to create bio text for
     * @return Formatted bio string
     */
    private String createBioText(DogProfile dog) {
        StringBuilder bio = new StringBuilder();
        addIfNotEmpty(bio, "Weight", dog.weight, " kg");
        addIfNotEmpty(bio, "Race", dog.race, "");
        addIfNotEmpty(bio, "Birthday", dog.birthday, "");
        addIfNotEmpty(bio, "Allergies", dog.allergies, "");
        addIfNotEmpty(bio, "Vaccines", dog.vaccines, "");
        String result = bio.toString().trim();
        if (!result.isEmpty()) {
            return result;
        }
        if (dog.bio != null && !dog.bio.isEmpty()) {
            return dog.bio;
        }
        return "No information available";
    }

    /**
     * Adds a field to the bio if the value is not empty.
     *
     * @param bio StringBuilder to append to
     * @param label Field label
     * @param value Field value
     * @param suffix Suffix to add after the value
     */
    private void addIfNotEmpty(StringBuilder bio, String label, String value, String suffix) {
        if (value != null && !value.isEmpty()) {
            if (bio.length() > 0) bio.append("\n");
            bio.append(label).append(": ").append(value).append(suffix);
        }
    }

    /**
     * Creates a DogProfile object from a Firestore document.
     *
     * @param document The Firestore document containing dog data
     * @return DogProfile object or null if creation fails
     */
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

            String age = extractStringOrNumber(document, "age", "0");
            String weight = extractStringOrNumber(document, "weight", "");

            String imageUrl = document.getString("profileImageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = document.getString("imageUrl");
            }
            if (imageUrl == null) imageUrl = "";
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
     * Handles dog selection from the RecyclerView by swapping positions and updating display.
     *
     * @param realIndex The index of the selected dog in the list
     */
    @Override
    public void onDogClick(int realIndex) {
        if (dogList.size() < 2) return;
        if (realIndex == 0) return;

        DogProfile temp = dogList.get(0);
        dogList.set(0, dogList.get(realIndex));
        dogList.set(realIndex, temp);

        currentDogProfile = dogList.get(0);
        currentDogProfile.setCurrent(true);
        dogList.get(realIndex).setCurrent(false);

        saveDogToPreferences(currentDogProfile);
        organizeDogsAndUpdateUI();
    }

    /**
     * Called when the fragment becomes visible to the user again.
     * Handles user changes, updates from preferences, and refreshes data from Firestore.
     */
    @Override
    public void onResume() {
        super.onResume();

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String savedUserId = sharedPreferences.getString("userId", null);

        if (currentUserId != null && !currentUserId.equals(savedUserId)) {
            sharedPreferences.edit().clear().apply();
            sharedPreferences.edit().putString("userId", currentUserId).apply();
            dogList.clear();
            adapter.notifyDataSetChanged();
            clearTopProfileDisplay();
        }

        updateFromPreferences();

        List<DogProfile> savedDogs = loadDogsListFromPreferences();
        if (!savedDogs.isEmpty()) {
            Log.d(TAG, "Loaded " + savedDogs.size() + " dogs from SharedPreferences");
            dogList.clear();
            dogList.addAll(savedDogs);
            organizeDogsAndUpdateUI();
        }

        loadAllDogProfilesFromFirestore();
    }

    /**
     * Updates the UI components with data from SharedPreferences.
     * Rebuilds bio text and loads profile image from local storage.
     */
    private void updateFromPreferences() {
        String name = sharedPreferences.getString("name", null);
        String age = sharedPreferences.getString("age", null);
        String race = sharedPreferences.getString("race", null);
        String weight = sharedPreferences.getString("weight", null);
        String allergies = sharedPreferences.getString("allergies", null);
        String vaccines = sharedPreferences.getString("vaccines", null);
        String birthday = sharedPreferences.getString("birthday", null);
        String bio = sharedPreferences.getString("bio", null);

        if (name != null && !name.isEmpty()) {
            userName.setText(name);
        }

        if (age != null && !age.isEmpty()) {
            dogAge.setText("Age: " + age);
        }

        StringBuilder bioBuilder = new StringBuilder();
        addIfNotEmpty(bioBuilder, "Weight", weight, " kg");
        addIfNotEmpty(bioBuilder, "Race", race, "");
        addIfNotEmpty(bioBuilder, "Birthday", birthday, "");
        addIfNotEmpty(bioBuilder, "Allergies", allergies, "");
        addIfNotEmpty(bioBuilder, "Vaccines", vaccines, "");

        String builtBio = bioBuilder.toString().trim();
        if (!builtBio.isEmpty()) {
            bioTextView.setText(builtBio);
        } else if (bio != null && !bio.isEmpty()) {
            bioTextView.setText(bio);
        } else {
            bioTextView.setText("No information available");
        }

        String imageUrl = getBestImageUrl(null);
        loadProfileImage(profilePic, imageUrl);
    }
}