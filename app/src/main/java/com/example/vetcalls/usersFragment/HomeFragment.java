package com.example.vetcalls.usersFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;
import com.bumptech.glide.Glide;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.DogProfileAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private Button editProfileButton;
    private TextView bioTextView, dogAge, userName;
    private SharedPreferences sharedPreferences;
    private ImageView profilePic;
    private RecyclerView dogRecyclerView;
    private DogProfileAdapter adapter;
    private List<DogProfile> dogList = new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        profilePic = view.findViewById(R.id.profilePic); // ❗ זה תיקון - השתמש במשתנה המחלקה
        editProfileButton = view.findViewById(R.id.editProfileButton);
        Button addDogButton = view.findViewById(R.id.addDogButton);

        addDogButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddDogProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        dogRecyclerView = view.findViewById(R.id.dogRecyclerView);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext())); // ✅ תוקן
        adapter = new DogProfileAdapter(requireContext(), dogList); // ✅ תוקן
        dogRecyclerView.setAdapter(adapter);

        loadAllDogProfilesFromFirestore();

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        loadSavedData();

        // Fragment result listener
        getParentFragmentManager().setFragmentResultListener("editProfileKey", this, (requestKey, bundle) -> {
            String updatedBio = bundle.getString("updatedBio");
            String updatedDogAge = bundle.getString("updatedDogAge");
            String updatedUserName = bundle.getString("updatedUserName");

            if (updatedBio != null) bioTextView.setText(updatedBio);
            if (updatedDogAge != null) dogAge.setText("Age: " + updatedDogAge);
            if (updatedUserName != null) userName.setText(updatedUserName);

            saveData(updatedUserName, updatedDogAge, updatedBio);
        });

        editProfileButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                // שמירה ב־SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("profileImageUrl", selectedImage.toString());
                editor.apply();

                // טעינה עגולה עם Glide
                Glide.with(requireContext())
                        .load(selectedImage)
                        .circleCrop()
                        .into((ImageView) getView().findViewById(R.id.profilePic));
            }
        }
    }


    // Load saved data from SharedPreferences
    private void loadSavedData() {
        userName.setText(sharedPreferences.getString("name", "No Name"));
        dogAge.setText("Age: " + sharedPreferences.getString("age", "Unknown"));
        bioTextView.setText(sharedPreferences.getString("bio", "No Information"));

        // בדיקת תמונת פרופיל
        String imageUrl = sharedPreferences.getString("imageUrl", null);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // טען תמונה שהוזנה ע"י המשתמש
            Glide.with(this)
                    .load(imageUrl)
                    .circleCrop()
                    .into(profilePic);
        } else {
            // הצג תמונת ברירת מחדל
            profilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
        }
    }


    // Save data to SharedPreferences
    private void saveData(String name, String age, String bio) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("age", age);
        editor.putString("bio", bio);
        editor.apply();
    }

    private void loadAllDogProfilesFromFirestore() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dogList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String age = doc.getString("age");
                        String bio = doc.getString("bio");
                        String imageUrl = doc.getString("imageUrl");

                        dogList.add(new DogProfile(name, age, bio, imageUrl));
                    }
                    adapter.notifyDataSetChanged();
                });
    }




}
