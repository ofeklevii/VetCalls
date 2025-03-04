package com.example.vetcalls.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vetcall.R;

public class HomeFragment extends Fragment {

    private Button editProfileButton;
    private TextView bioTextView, dogAge, userName;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        bioTextView = view.findViewById(R.id.bioText);
        dogAge = view.findViewById(R.id.dogAge);
        userName = view.findViewById(R.id.userName);
        editProfileButton = view.findViewById(R.id.editProfileButton);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Load saved data
        loadSavedData();

        // Listen for fragment result
        getParentFragmentManager().setFragmentResultListener("editProfileKey", this, (requestKey, bundle) -> {
            String updatedBio = bundle.getString("updatedBio");
            String updatedDogAge = bundle.getString("updatedDogAge");
            String updatedUserName = bundle.getString("updatedUserName");
            if (updatedBio != null) {
                bioTextView.setText(updatedBio);
            }
            if (updatedDogAge != null) {
                dogAge.setText(updatedDogAge);
            }
            if (updatedUserName != null) {
                userName.setText(updatedUserName);
            }

            // Save to SharedPreferences for persistence
            saveData(updatedUserName, updatedDogAge, updatedBio);
        });

        // Open EditProfileFragment on button click
        editProfileButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    // Load saved data from SharedPreferences
    private void loadSavedData() {
        userName.setText(sharedPreferences.getString("name", "No Name"));
        dogAge.setText(sharedPreferences.getString("age", "Unknown"));
        bioTextView.setText(sharedPreferences.getString("bio", "No Information"));
    }

    // Save data to SharedPreferences
    private void saveData(String name, String age, String bio) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("age", age);
        editor.putString("bio", bio);
        editor.apply();
    }
}

