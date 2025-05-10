package com.example.vetcalls.usersFragment;

import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment implements DogProfileAdapter.OnDogClickListener {

    private static final String TAG = "HomeFragment";
    private Button editProfileButton, addDogButton;
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

        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        profilePic = view.findViewById(R.id.profilePic);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        addDogButton = view.findViewById(R.id.addDogButton);

        addDogButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddDogProfileFragment())
                .addToBackStack(null)
                .commit());

        dogRecyclerView = view.findViewById(R.id.dogRecyclerView);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DogProfileAdapter(requireContext(), dogList, this);
        dogRecyclerView.setAdapter(adapter);

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        addTestDogIfEmpty();

        loadSavedData();
        loadAllDogProfilesFromFirestore();

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

            if (updatedBio != null) bioTextView.setText(updatedBio);
            if (updatedDogAge != null) dogAge.setText("Age: " + updatedDogAge);
            if (updatedUserName != null) userName.setText(updatedUserName);
            if (updatedImageUri != null) {
                Glide.with(requireContext()).load(Uri.parse(updatedImageUri)).circleCrop().into(profilePic);
                sharedPreferences.edit().putString("profileImageUrl", updatedImageUri).apply();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("name", updatedUserName);
            editor.putString("age", updatedDogAge);
            editor.putString("bio", updatedBio);
            if (race != null) editor.putString("race", race);
            if (birthday != null) editor.putString("birthday", birthday);
            if (weight != null) editor.putString("weight", weight);
            if (allergies != null) editor.putString("allergies", allergies);
            if (vaccines != null) editor.putString("vaccines", vaccines);
            if (dogId != null) editor.putString("dogId", dogId);
            editor.apply();

            if (dogId != null) {
                currentDogProfile = new DogProfile(
                        dogId,
                        updatedUserName,
                        updatedDogAge,
                        updatedBio,
                        updatedImageUri,
                        race,
                        birthday,
                        weight,
                        allergies,
                        vaccines,
                        FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        null
                );
                currentDogProfile.setCurrent(true);
                adapter.setCurrentDog(currentDogProfile);
            }

            loadAllDogProfilesFromFirestore();
        });

        editProfileButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (currentDogProfile != null && currentDogProfile.getId() != null) {
                args.putString("dogId", currentDogProfile.getId());
            } else {
                String savedDogId = sharedPreferences.getString("dogId", null);
                if (savedDogId != null) {
                    args.putString("dogId", savedDogId);
                }
            }

            EditProfileFragment editFragment = new EditProfileFragment();
            editFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void addTestDogIfEmpty() {
        if (dogList.isEmpty()) {
            DogProfile testDog = new DogProfile(
                    "test123",
                    "TestDog",
                    "5",
                    "Test Bio",
                    null,
                    "Golden Retriever",
                    "2020-01-01",
                    "25",
                    "None",
                    "None",
                    FirebaseAuth.getInstance().getCurrentUser() != null ?
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : "",
                    null
            );
            testDog.setCurrent(true);
            dogList.add(testDog);
            Log.d(TAG, "Added test dog to list: " + testDog.getName());
        }
    }

    private void loadSavedData() {
        userName.setText(sharedPreferences.getString("name", "No Name"));
        dogAge.setText("Age: " + sharedPreferences.getString("age", "Unknown"));
        bioTextView.setText(sharedPreferences.getString("bio", "No Information"));

        String imageUrl = sharedPreferences.getString("profileImageUrl", null);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(Uri.parse(imageUrl)).circleCrop().into(profilePic);
        } else {
            profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }

        String dogId = sharedPreferences.getString("dogId", null);
        if (dogId != null) {
            loadCurrentDogFromFirestore(dogId);
        }
    }

    private void loadCurrentDogFromFirestore(String dogId) {
        Log.d(TAG, "Loading current dog from Firestore: " + dogId);
        FirebaseFirestore.getInstance()
                .collection("DogProfiles")
                .document(dogId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            currentDogProfile = createDogProfileFromDocument(documentSnapshot);
                            currentDogProfile.setCurrent(true);
                            Log.d(TAG, "Current dog loaded: " + currentDogProfile.getName());

                            if (adapter != null) {
                                adapter.setCurrentDog(currentDogProfile);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating dog profile from document: " + e.getMessage());
                            Toast.makeText(requireContext(), "Error loading dog profile", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Dog document doesn't exist: " + dogId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog: " + e.getMessage());
                });
    }

    private void loadAllDogProfilesFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null!");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading dogs for user: " + userId);

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Got result with " + queryDocumentSnapshots.size() + " documents");
                    dogList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        addTestDogIfEmpty();
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d(TAG, "Processing dog document: " + doc.getId());

                        try {
                            // בגלל בעיות ההמרה, נשתמש בשיטה ידנית במקום toObject()
                            String name = doc.getString("name");

                            // טיפול בשדה הגיל - יכול להיות מספר או מחרוזת
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

                            // אם הכלב הנוכחי הוא זה שמוצג עכשיו, מעדכנים את הסטטוס שלו
                            if (currentDogProfile != null &&
                                    dogProfile.getId() != null &&
                                    dogProfile.getId().equals(currentDogProfile.getId())) {
                                dogProfile.setCurrent(true);
                            }

                            dogList.add(dogProfile);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing dog document: " + e.getMessage());
                        }
                    }

                    // אם יש כלב נוכחי, נעביר אותו לתוך האדפטר
                    if (currentDogProfile != null) {
                        adapter.setCurrentDog(currentDogProfile);
                    } else if (!dogList.isEmpty()) {
                        currentDogProfile = dogList.get(0);
                        currentDogProfile.setCurrent(true);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog profiles: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dogs", Toast.LENGTH_SHORT).show();
                });
    }

    private DogProfile createDogProfileFromDocument(DocumentSnapshot document) {
        try {
            // במקום להשתמש ב-toObject, נבנה את האובייקט בעצמנו
            String id = document.getId();

            // טיפול בשדה הגיל - יכול להיות מספר או מחרוזת
            String age;
            Object ageObj = document.get("age");
            if (ageObj instanceof Long) {
                age = String.valueOf(document.getLong("age"));
            } else if (ageObj instanceof String) {
                age = document.getString("age");
            } else {
                age = "Unknown";
            }

            return new DogProfile(
                    id,
                    document.getString("name"),
                    age,
                    document.getString("bio"),
                    document.getString("imageUrl"),
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
            // במקרה של שגיאה, ננסה לבנות אובייקט חלקי
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
        Log.d(TAG, "Clicked on dog: " + dogProfile.getName());

        if (currentDogProfile != null) {
            currentDogProfile.setCurrent(false);
        }

        currentDogProfile = dogProfile;
        dogProfile.setCurrent(true);

        adapter.setCurrentDog(dogProfile);

        loadAllDogProfilesFromFirestore();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllDogProfilesFromFirestore();
        loadSavedData(); // רענון המידע מה-SharedPreferences
    }
}