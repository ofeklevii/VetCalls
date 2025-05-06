package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class AddDogProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView editProfilePic;
    private EditText editName, editBirthday, editWeight, editRace, editAllergies, editVaccines;
    private Button changeProfilePicButton, saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    public AddDogProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_dog_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("dog_profile_images");

        editProfilePic = view.findViewById(R.id.editProfilePic);
        editName = view.findViewById(R.id.editName);
        editBirthday = view.findViewById(R.id.editBirthday);
        editWeight = view.findViewById(R.id.editWeight);
        editRace = view.findViewById(R.id.editRace);
        editAllergies = view.findViewById(R.id.editAllergies);
        editVaccines = view.findViewById(R.id.editVaccines);
        changeProfilePicButton = view.findViewById(R.id.changeProfilePicButton);
        saveButton = view.findViewById(R.id.saveButton);

        changeProfilePicButton.setOnClickListener(v -> openImagePicker());

        saveButton.setOnClickListener(v -> saveDogProfile());

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            editProfilePic.setImageURI(imageUri);
        }
    }

    private void saveDogProfile() {
        String name = editName.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();
        String race = editRace.getText().toString().trim();
        String allergies = editAllergies.getText().toString().trim();
        String vaccines = editVaccines.getText().toString().trim();

        if (name.isEmpty() || birthday.isEmpty() || weight.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String dogId = UUID.randomUUID().toString();
        String userId = auth.getCurrentUser().getUid();

        if (imageUri != null) {
            FirestoreUserHelper.uploadDogProfileImage(imageUri, dogId, new FirestoreUserHelper.OnImageUploadListener() {
                @Override
                public void onUploadSuccess(String imageUrl) {
                    saveDogDataToHelper(dogId, name, race, birthday, weight, allergies, vaccines, userId, imageUrl);
                }

                @Override
                public void onUploadFailed(Exception e) {
                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveDogDataToHelper(dogId, name, race, birthday, weight, allergies, vaccines, userId, null);
        }

    }

    private int calculateDogAge(String birthday) {
        if (birthday.isEmpty()) return 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date birthDate = dateFormat.parse(birthday);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


    private void saveDogDataToHelper(String dogId, String name, String race, String birthday,
                                     String weight, String allergies, String vaccines,
                                     String ownerId, @Nullable String imageUrl) {
        int age = calculateDogAge(birthday); // תוכל לכתוב פונקציה שמחשבת גיל לפי תאריך

        FirestoreUserHelper.addDogProfile(
                dogId, name, race, age, birthday, weight,
                allergies, vaccines, "", // bio ריק כרגע
                ownerId, null, // vetId יאוכלס בעתיד אם תרצה
                imageUrl
        );

        Toast.makeText(getContext(), "Dog profile saved!", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }


    private void saveToFirestore(String dogId, String name, String race, String birthday,
                                 String weight, String allergies, String vaccines,
                                 String ownerId, @Nullable String imageUrl) {
        int age = calculateDogAge(birthday);

        Map<String, Object> dogData = new HashMap<>();
        dogData.put("dogId", dogId);
        dogData.put("name", name);
        dogData.put("race", race);
        dogData.put("birthday", birthday);
        dogData.put("age", age);
        dogData.put("weight", weight);
        dogData.put("allergies", allergies);
        dogData.put("vaccines", vaccines);
        dogData.put("bio", ""); // ביו ריק כרגע
        dogData.put("ownerId", ownerId);
        dogData.put("vetId", null); // יתמלא בעתיד
        dogData.put("imageUrl", imageUrl);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // שמירה תחת DogProfiles
        db.collection("DogProfiles").document(dogId)
                .set(dogData)
                .addOnSuccessListener(aVoid -> {
                    // גם תחת Users/{userId}/Dogs
                    db.collection("Users").document(ownerId)
                            .collection("Dogs").document(dogId)
                            .set(dogData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Dog profile saved!", Toast.LENGTH_SHORT).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to save under user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save dog profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
