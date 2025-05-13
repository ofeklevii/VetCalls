package com.example.vetcalls.usersFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.DogProfileAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.activities.LoginActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class HomeFragment extends Fragment implements DogProfileAdapter.OnDogClickListener {

    private static final String TAG = "HomeFragment";
    private Button editProfileButton, addDogButton, deleteAccountButton;
    private TextView bioTextView, dogAge, userName;
    private ImageView profilePic;
    private RecyclerView dogRecyclerView;
    private DogProfileAdapter adapter;
    private List<DogProfile> dogList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private DogProfile currentDogProfile; // פרופיל הכלב הנוכחי המוצג למעלה

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI components
        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        profilePic = view.findViewById(R.id.profilePic);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        addDogButton = view.findViewById(R.id.addDogButton);
        deleteAccountButton = view.findViewById(R.id.deleteAccountButton);

        // Set up Add Dog button to launch Add Dog Fragment
        addDogButton.setOnClickListener(v -> {
            // Create new EditProfileFragment with null dogId to indicate new dog
            EditProfileFragment addDogFragment = new EditProfileFragment();
            Bundle args = new Bundle();
            args.putString("dogId", ""); // Empty string indicates new dog
            addDogFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, addDogFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Set up RecyclerView for dog profiles
        dogRecyclerView = view.findViewById(R.id.dogRecyclerView);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DogProfileAdapter(requireContext(), dogList, this);
        dogRecyclerView.setAdapter(adapter);

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Clear initial display
        clearTopProfileDisplay();

        // Load data from Firestore
        loadAllDogProfilesFromFirestore();

        // Handle results from EditProfileFragment
        getParentFragmentManager().setFragmentResultListener("editProfileKey", this, (requestKey, bundle) -> {
            String updatedBio = bundle.getString("updatedBio");
            String updatedDogAge = bundle.getString("updatedDogAge");
            String updatedUserName = bundle.getString("updatedUserName");
            String updatedImageUri = bundle.getString("updatedImageUri");
            String race = bundle.getString("race");
            String birthday = bundle.getString("birthday");
            String weight = bundle.getString("weight");
            String allergies = bundle.getString("allergies");
            String vaccines = bundle.getString("vaccines");
            String dogId = bundle.getString("dogId");
            boolean isNewDog = bundle.getBoolean("isNewDog", false);

            Log.d(TAG, "Received fragment result with dogId: " + dogId + ", isNewDog: " + isNewDog);

            // Create updated dog profile object
            if (dogId != null) {
                // Get image URL from either updated or previous value
                String imageUrl = updatedImageUri;
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = sharedPreferences.getString("profileImageUrl", null);
                }

                DogProfile updatedDog = new DogProfile(
                        dogId,
                        updatedUserName,
                        updatedDogAge,
                        updatedBio,
                        imageUrl,
                        race,
                        birthday,
                        weight,
                        allergies,
                        vaccines,
                        FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        null
                );

                // Update current dog profile
                currentDogProfile = updatedDog;
                currentDogProfile.setCurrent(true);

                Log.d(TAG, "Created updated dog profile: " + updatedDog.getName());

                // Update dog list - check if dog already exists
                boolean dogExists = false;
                for (int i = 0; i < dogList.size(); i++) {
                    if (dogList.get(i).getId() != null && dogList.get(i).getId().equals(dogId)) {
                        // Dog exists, update it and mark as current
                        dogList.set(i, updatedDog);
                        dogExists = true;
                        Log.d(TAG, "Updated existing dog in list");
                        break;
                    }
                }

                // If it's a new dog, add to list
                if (!dogExists) {
                    dogList.add(updatedDog);
                    Log.d(TAG, "Added new dog to list");
                }

                // Update UI immediately
                organizeDogsAndUpdateUI();

                // Save the current dog ID in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("dogId", dogId);
                editor.apply();

                // Refresh from server after immediate refresh
                loadAllDogProfilesFromFirestore();
            }
        });

        // Set up edit profile button
        editProfileButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (currentDogProfile != null && currentDogProfile.getId() != null) {
                args.putString("dogId", currentDogProfile.getId());
                Log.d(TAG, "Launching edit with current dog: " + currentDogProfile.getId());
            } else {
                String savedDogId = sharedPreferences.getString("dogId", null);
                if (savedDogId != null) {
                    args.putString("dogId", savedDogId);
                    Log.d(TAG, "Launching edit with saved dog: " + savedDogId);
                } else {
                    Log.d(TAG, "Launching edit with new dog (no ID)");
                }
            }

            EditProfileFragment editFragment = new EditProfileFragment();
            editFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        });

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        return view;
    }

    // Show confirmation dialog for account deletion
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("מחיקת חשבון")
                .setMessage("האם אתה בטוח שברצונך למחוק את החשבון שלך? פעולה זו תמחק את כל הנתונים הקשורים לחשבון שלך ואינה ניתנת לביטול.")
                .setPositiveButton("כן, מחק", (dialog, which) -> {
                    // Show loading dialog
                    AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                            .setMessage("מוחק חשבון...")
                            .setCancelable(false)
                            .create();
                    loadingDialog.show();

                    // Call delete function
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String userId = currentUser.getUid();
                        FirestoreUserHelper.deleteUserCompletely(userId,
                                () -> {
                                    // Success
                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(requireContext(), "החשבון נמחק בהצלחה", Toast.LENGTH_LONG).show();

                                    // Return to login screen
                                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    requireActivity().finish();
                                },
                                () -> {
                                    // Failure
                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(requireContext(), "שגיאה במחיקת החשבון, נסה שוב מאוחר יותר", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // If no user is logged in
                        loadingDialog.dismiss();
                        Toast.makeText(requireContext(), "אתה לא מחובר למערכת", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("לא, בטל", null)
                .show();
    }

    // Function to clear the top profile display
    private void clearTopProfileDisplay() {
        userName.setText("Username");
        dogAge.setText("Age");
        bioTextView.setText("Your bio goes here...");
        profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        currentDogProfile = null;
    }

    private void loadAllDogProfilesFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null!");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading dogs for user: " + userId);

        // Clear existing list
        dogList.clear();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Got result with " + queryDocumentSnapshots.size() + " documents");

                    // If no dogs at all - leave screen empty
                    if (queryDocumentSnapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        clearTopProfileDisplay();
                        return;
                    }

                    // Check if collection contains only references or all data
                    DocumentSnapshot firstDoc = queryDocumentSnapshots.getDocuments().get(0);

                    // Check if has only dogId (then it's a reference)
                    if (firstDoc.contains("dogId") && !firstDoc.contains("bio")) {
                        // New structure - first read references
                        List<String> dogIds = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String dogId = doc.getString("dogId");
                            if (dogId != null) {
                                dogIds.add(dogId);
                            }
                        }

                        // Now load dogs from DogProfiles collection
                        if (!dogIds.isEmpty()) {
                            loadDogsFromDogProfiles(dogIds);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        // Old structure - everything is in subcollection
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                String name = doc.getString("name");

                                // Handle age field - can be number or string
                                String age;
                                Object ageObj = doc.get("age");
                                if (ageObj instanceof Long) {
                                    age = String.valueOf(doc.getLong("age"));
                                } else if (ageObj instanceof String) {
                                    age = doc.getString("age");
                                } else {
                                    age = "Unknown";
                                }

                                String bio = doc.getString("bio");
                                String imageUrl = doc.getString("imageUrl");
                                String race = doc.getString("race");
                                String birthday = doc.getString("birthday");
                                String weight = doc.getString("weight");
                                String allergies = doc.getString("allergies");
                                String vaccines = doc.getString("vaccines");
                                String ownerId = doc.getString("ownerId");
                                String vetId = doc.getString("vetId");

                                DogProfile dogProfile = new DogProfile(
                                        doc.getId(),
                                        name != null ? name : "",
                                        age != null ? age : "",
                                        bio,
                                        imageUrl,
                                        race,
                                        birthday,
                                        weight,
                                        allergies,
                                        vaccines,
                                        ownerId,
                                        vetId
                                );

                                // Add dog to list
                                dogList.add(dogProfile);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing dog document: " + e.getMessage());
                            }
                        }

                        // Organize list and update UI
                        organizeDogsAndUpdateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog profiles: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dogs", Toast.LENGTH_SHORT).show();
                });
    }

    // Function to load dogs from DogProfiles collection - prevents duplicates
    private void loadDogsFromDogProfiles(List<String> dogIds) {
        // Clear existing list
        dogList.clear();

        // Track dog IDs already loaded
        Set<String> loadedDogIds = new HashSet<>();

        // Track number of dogs loaded
        final int[] loadedCount = {0};
        final int totalCount = dogIds.size();

        for (String dogId : dogIds) {
            // Skip dogs already loaded
            if (loadedDogIds.contains(dogId)) {
                // Update loading counter
                loadedCount[0]++;
                if (loadedCount[0] >= totalCount) {
                    organizeDogsAndUpdateUI();
                }
                continue;
            }

            // Add ID to tracking
            loadedDogIds.add(dogId);

            FirebaseFirestore.getInstance()
                    .collection("DogProfiles")
                    .document(dogId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            try {
                                DogProfile dogProfile = createDogProfileFromDocument(document);

                                // Prevent adding dog with same ID
                                boolean isDuplicate = false;
                                for (DogProfile existingDog : dogList) {
                                    if (existingDog.getId() != null &&
                                            existingDog.getId().equals(dogProfile.getId())) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }

                                if (!isDuplicate) {
                                    // Check if this is the current dog
                                    String currentDogId = sharedPreferences.getString("dogId", null);
                                    if (currentDogId != null && currentDogId.equals(dogProfile.getId())) {
                                        currentDogProfile = dogProfile;
                                        dogProfile.setCurrent(true);
                                    }

                                    // Add dog to list
                                    dogList.add(dogProfile);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating dog profile: " + e.getMessage());
                            }
                        }

                        // Check if all dogs are loaded
                        loadedCount[0]++;
                        if (loadedCount[0] >= totalCount) {
                            // Organize list and update UI
                            organizeDogsAndUpdateUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading dog profile: " + e.getMessage());

                        // Even in case of error, continue checking if all dogs loaded
                        loadedCount[0]++;
                        if (loadedCount[0] >= totalCount) {
                            organizeDogsAndUpdateUI();
                        }
                    });
        }
    }

    // Updated function to organize list and update user interface
    private void organizeDogsAndUpdateUI() {
        if (dogList.isEmpty()) {
            clearTopProfileDisplay();
            adapter.notifyDataSetChanged();
            return;
        }

        // Find current dog
        String savedDogId = sharedPreferences.getString("dogId", null);
        DogProfile topDogProfile = null;
        boolean foundCurrentDog = false;

        // If there's a saved dog ID in preferences, find it in the list
        if (savedDogId != null && !savedDogId.isEmpty()) {
            for (DogProfile dog : dogList) {
                if (dog.getId() != null && dog.getId().equals(savedDogId)) {
                    // Found the saved dog
                    topDogProfile = dog;
                    currentDogProfile = dog;
                    dog.setCurrent(true);
                    foundCurrentDog = true;
                    break;
                }
            }
        }

        // If saved dog not found (or no saved dog), take the first one
        if (!foundCurrentDog && !dogList.isEmpty()) {
            topDogProfile = dogList.get(0);
            currentDogProfile = topDogProfile;
            currentDogProfile.setCurrent(true);

            // Save current dog ID
            sharedPreferences.edit()
                    .putString("dogId", currentDogProfile.getId())
                    .apply();
        }

        // Update top display
        if (topDogProfile != null) {
            updateTopProfileDisplay(topDogProfile);
        }

        // Create filtered list without duplicates and without current dog
        List<DogProfile> filteredList = new ArrayList<>();

        // Track dog IDs already added
        Set<String> addedDogIds = new HashSet<>();

        // If there's a current dog, add its ID to tracking
        if (currentDogProfile != null && currentDogProfile.getId() != null) {
            addedDogIds.add(currentDogProfile.getId());
        }

        // Add dogs to filtered list - only if they're not the current dog and not already added
        for (DogProfile dog : dogList) {
            if (dog.getId() == null) continue; // Skip dogs without ID

            // If dog is not current dog and not already added
            if (!addedDogIds.contains(dog.getId())) {
                // Mark this dog as not current
                dog.setCurrent(false);
                // Add dog to filtered list
                filteredList.add(dog);
                // Add ID to tracking
                addedDogIds.add(dog.getId());
            }
        }

        // Update list in adapter
        adapter.updateDogList(filteredList);
        adapter.notifyDataSetChanged();
    }

    // Function to update top display with dog data
    private void updateTopProfileDisplay(DogProfile dog) {
        if (dog != null) {
            // Dog name
            userName.setText(dog.getName());

            // Dog age
            String age = dog.getAge();
            if (age != null && !age.isEmpty()) {
                dogAge.setText("Age: " + age);
            } else {
                dogAge.setText("Age: Unknown");
            }

            // Dog bio
            String bio = dog.getBio();
            if (bio != null && !bio.isEmpty()) {
                bioTextView.setText(bio);
            } else {
                // Create bio from available data
                StringBuilder bioBuilder = new StringBuilder();

                if (dog.getRace() != null && !dog.getRace().isEmpty()) {
                    bioBuilder.append("Race: ").append(dog.getRace()).append("\n");
                }

                if (dog.getWeight() != null && !dog.getWeight().isEmpty()) {
                    bioBuilder.append("Weight: ").append(dog.getWeight()).append(" kg\n");
                }

                if (dog.getAllergies() != null && !dog.getAllergies().isEmpty()) {
                    bioBuilder.append("Allergies: ").append(dog.getAllergies()).append("\n");
                }

                if (dog.getVaccines() != null && !dog.getVaccines().isEmpty()) {
                    bioBuilder.append("Vaccines: ").append(dog.getVaccines());
                }

                String builtBio = bioBuilder.toString().trim();
                if (!builtBio.isEmpty()) {
                    bioTextView.setText(builtBio);
                } else {
                    bioTextView.setText("No information available");
                }
            }

            // Profile picture
            String imageUrl = dog.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(imageUrl)
                        .circleCrop()
                        .into(profilePic);
            } else {
                profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
            }
        }
    }

    private DogProfile createDogProfileFromDocument(DocumentSnapshot document) {
        try {
            // Instead of using toObject, build object manually
            String id = document.getId();

            // Handle age field - can be number or string
            String age;
            Object ageObj = document.get("age");
            if (ageObj instanceof Long) {
                age = String.valueOf(document.getLong("age"));
            } else if (ageObj instanceof String) {
                age = document.getString("age");
            } else {
                age = "Unknown";
            }

            // Try both profileImageUrl and imageUrl fields
            String imageUrl = document.getString("profileImageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = document.getString("imageUrl");
            }

            // Create dog with all data
            return new DogProfile(
                    id,
                    document.getString("name"),
                    age,
                    document.getString("bio"),
                    imageUrl,
                    document.getString("race"),
                    document.getString("birthday"),
                    document.getString("weight"),
                    document.getString("allergies"),
                    document.getString("vaccines"),
                    document.getString("ownerId"),
                    document.getString("vetId")
            );
        } catch (Exception e) {
            Log.e(TAG, "Error in createDogProfileFromDocument: " + e.getMessage());
            // In case of error, try to build partial object
            return new DogProfile(
                    document.getId(),
                    document.getString("name") != null ? document.getString("name") : "Unknown Dog",
                    "Unknown",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }
    }

    @Override
    public void onDogClick(DogProfile dogProfile) {
        Log.d(TAG, "Dog clicked: " + dogProfile.getName());

        // Save the new dog ID as current
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("dogId", dogProfile.getId());

        // Also save other details for quick access
        editor.putString("name", dogProfile.getName());
        editor.putString("age", dogProfile.getAge());
        editor.putString("race", dogProfile.getRace());
        editor.putString("bio", dogProfile.getBio());
        editor.putString("birthday", dogProfile.getBirthday());
        editor.putString("weight", dogProfile.getWeight());
        editor.putString("allergies", dogProfile.getAllergies());
        editor.putString("vaccines", dogProfile.getVaccines());
        editor.putString("profileImageUrl", dogProfile.getImageUrl());
        editor.apply();

        // Update current dog reference
        currentDogProfile = dogProfile;

        // Update top display with clicked dog
        updateTopProfileDisplay(dogProfile);

        // Reorganize the list (moved clicked dog to top)
        organizeDogsAndUpdateUI();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh data from both SharedPreferences and Firestore
        String name = sharedPreferences.getString("name", null);
        String age = sharedPreferences.getString("age", null);
        String bio = sharedPreferences.getString("bio", null);
        String profileImageUrl = sharedPreferences.getString("profileImageUrl", null);

        // Update display if there's data
        if (name != null && !name.isEmpty()) {
            userName.setText(name);
        }
        if (age != null && !age.isEmpty()) {
            dogAge.setText("Age: " + age);
        }
        if (bio != null && !bio.isEmpty()) {
            bioTextView.setText(bio);
        }
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .into(profilePic);
        }

        // Refresh list from server
        loadAllDogProfilesFromFirestore();
    }
}