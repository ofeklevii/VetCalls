package com.example.vetcalls.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.vetcall.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditProfileFragment extends Fragment {

    private EditText editName, editBirthday, editWeight, editRace, editAllergies, editVaccines;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize fields
        editName = view.findViewById(R.id.editName);
        editBirthday = view.findViewById(R.id.editBirthday);
        editWeight = view.findViewById(R.id.editWeight);
        editRace = view.findViewById(R.id.editRace);
        editAllergies = view.findViewById(R.id.editAllergies);
        editVaccines = view.findViewById(R.id.editVaccines);
        saveButton = view.findViewById(R.id.saveButton);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Load saved values
        loadSavedData();

        // Save button click listener
        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String birthday = editBirthday.getText().toString().trim();
            String weight = editWeight.getText().toString().trim();
            String race = editRace.getText().toString().trim();
            String allergies = editAllergies.getText().toString().trim();
            String vaccines = editVaccines.getText().toString().trim();

            // Calculate dog's age
            String age = calculateDogAge(birthday);

            // Format bio text
            String bioText = "Weight: " + weight + " kg" +
                    "\nRace: " + race +
                    "\nAllergies: " + allergies +
                    "\nVaccines: " + vaccines;

            // Save data to SharedPreferences
            saveData(name, birthday, weight, race, allergies, vaccines, age, bioText);

            // Send data back to HomeFragment
            Bundle result = new Bundle();
            result.putString("updatedBio", bioText);
            result.putString("updatedDogAge", age);
            result.putString("updatedUserName", name);
            getParentFragmentManager().setFragmentResult("editProfileKey", result);

            // Navigate back to HomeFragment
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.popBackStack();
        });

        return view;
    }

    // Function to calculate dog's age based on birth date
    private String calculateDogAge(String birthday) {
        if (birthday.isEmpty()) return "Unknown";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date birthDate = dateFormat.parse(birthday);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--; // If birthday hasn't occurred yet this year
            }

            return String.valueOf(age);
        } catch (ParseException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    // Save data to SharedPreferences
    private void saveData(String name, String birthday, String weight, String race, String allergies, String vaccines, String age, String bio) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("birthday", birthday);
        editor.putString("weight", weight);
        editor.putString("race", race);
        editor.putString("allergies", allergies);
        editor.putString("vaccines", vaccines);
        editor.putString("age", age);
        editor.putString("bio", bio);
        editor.apply();
    }

    // Load saved data from SharedPreferences
    private void loadSavedData() {
        editName.setText(sharedPreferences.getString("name", ""));
        editBirthday.setText(sharedPreferences.getString("birthday", ""));
        editWeight.setText(sharedPreferences.getString("weight", ""));
        editRace.setText(sharedPreferences.getString("race", ""));
        editAllergies.setText(sharedPreferences.getString("allergies", ""));
        editVaccines.setText(sharedPreferences.getString("vaccines", ""));
    }
}
