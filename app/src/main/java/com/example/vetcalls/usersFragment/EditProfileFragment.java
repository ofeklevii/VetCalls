package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.obj.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class EditProfileFragment extends Fragment {

    private EditText editName, editBirthday, editWeight, editRace, editAllergies, editVaccines;
    private Button saveButton, changeProfilePicButton;
    private ImageView editProfilePic;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private User user;
    private String vetId;
    private Uri selectedImageUri;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private String dogId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        editName = view.findViewById(R.id.editName);
        editBirthday = view.findViewById(R.id.editBirthday);
        editWeight = view.findViewById(R.id.editWeight);
        editRace = view.findViewById(R.id.editRace);
        editAllergies = view.findViewById(R.id.editAllergies);
        editVaccines = view.findViewById(R.id.editVaccines);
        saveButton = view.findViewById(R.id.saveButton);
        changeProfilePicButton = view.findViewById(R.id.changeProfilePicButton);
        editProfilePic = view.findViewById(R.id.editProfilePic);


        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        loadSavedData();

        saveButton.setOnClickListener(v -> saveProfile());
        changeProfilePicButton.setOnClickListener(v -> showImagePickerDialog());

        return view;
    }

    private void saveProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String race = editRace.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();
        String allergies = editAllergies.getText().toString().trim();
        String vaccines = editVaccines.getText().toString().trim();
        String ownerId = currentUser.getUid();
        String age = calculateDogAge(birthday);
        dogId = sharedPreferences.getString("dogId", UUID.randomUUID().toString());
        String bio = "";
        String vet = (vetId != null && !vetId.isEmpty()) ? vetId : "";

        if (name.isEmpty() || race.isEmpty() || birthday.isEmpty() || weight.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // בשלב זה אנחנו שולחים null עד שה-URL האמיתי יעלה ל-Firebase
        FirestoreUserHelper.addDogProfile(
                dogId,
                name,
                race,
                Integer.parseInt(age),
                birthday,
                weight,
                allergies,
                vaccines,
                bio,
                ownerId,
                vet,
                null  // imageUrl עדיין לא ידוע בשלב הזה
        );

        saveLocally(name, birthday, weight, race, allergies, vaccines, age, bio, dogId);

        if (selectedImageUri != null) {
            uploadImageToFirebase(selectedImageUri); // כאן תתבצע העלאה, ובהמשך תעודכן ה-URL
        }

        Toast.makeText(getContext(), "Profile saved", Toast.LENGTH_SHORT).show();
        navigateToHome(bio, age, name);
    }


    private String calculateDogAge(String birthday) {
        if (birthday.isEmpty()) return "0";
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
            return String.valueOf(age);
        } catch (ParseException e) {
            e.printStackTrace();
            return "0";
        }
    }

    private void saveLocally(String name, String birthday, String weight, String race, String allergies, String vaccines, String age, String bio, String dogId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("birthday", birthday);
        editor.putString("weight", weight);
        editor.putString("race", race);
        editor.putString("allergies", allergies);
        editor.putString("vaccines", vaccines);
        editor.putString("age", age);
        editor.putString("bio", bio);
        editor.putString("dogId", dogId);
        if (vetId != null) editor.putString("vetId", vetId);
        editor.apply();
    }

    private void navigateToHome(String bioText, String age, String name) {
        Bundle result = new Bundle();
        result.putString("updatedBio", bioText);
        result.putString("updatedDogAge", age);
        result.putString("updatedUserName", name);
        getParentFragmentManager().setFragmentResult("editProfileKey", result);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .addToBackStack(null)
                .commit();
    }

    private void loadSavedData() {
        user = new User(
                sharedPreferences.getString("email", ""),
                sharedPreferences.getString("vaccines", ""),
                sharedPreferences.getString("allergies", ""),
                sharedPreferences.getString("race", ""),
                sharedPreferences.getString("weight", ""),
                sharedPreferences.getString("name", ""),
                sharedPreferences.getString("birthday", ""),
                sharedPreferences.getBoolean("isVet", false)
        );

        vetId = sharedPreferences.getString("vetId", "");
        dogId = sharedPreferences.getString("dogId", "");

        editName.setText(user.getName());
        editBirthday.setText(user.getBirthday());
        editWeight.setText(user.getWeight());
        editRace.setText(user.getRace());
        editAllergies.setText(user.getAllergies());
        editVaccines.setText(user.getVaccines());
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickImageFromGallery();
                    else takePhotoWithCamera();
                })
                .show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void takePhotoWithCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                selectedImageUri = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                selectedImageUri = getImageUriFromBitmap(requireContext(), photo);
            }
            if (selectedImageUri != null) {
                editProfilePic.setImageURI(selectedImageUri); // הצגת התמונה במסך
                Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "TempImage", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("dog_profile_images/" + dogId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    // עדכון תמונה ב-Firestore
                    FirebaseFirestore.getInstance()
                            .collection("DogProfiles")
                            .document(dogId)
                            .update("profileImageUrl", downloadUrl);

                    // שמירה מקומית
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("profileImageUrl", downloadUrl);
                    editor.apply();

                    // טעינה עגולה עם Glide
                    Glide.with(requireContext())
                            .load(downloadUrl)
                            .circleCrop()
                            .into(editProfilePic);

                    Toast.makeText(getContext(), "Profile picture uploaded", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}
