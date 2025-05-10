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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.FirestoreUserHelper;
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

public class AddDogProfileFragment extends Fragment {

    private static final String TAG = "AddDogProfileFragment";
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private EditText editName, editBirthday, editWeight, editRace, editAllergies, editVaccines;
    private Button changeProfilePicButton, saveButton, cancelButton;
    private ImageView editProfilePic;
    private Uri selectedImageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private String dogId;
    private String vetId;

    public AddDogProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_dog_profile, container, false);

        editProfilePic = view.findViewById(R.id.editProfilePic);
        editName = view.findViewById(R.id.editName);
        editBirthday = view.findViewById(R.id.editBirthday);
        editWeight = view.findViewById(R.id.editWeight);
        editRace = view.findViewById(R.id.editRace);
        editAllergies = view.findViewById(R.id.editAllergies);
        editVaccines = view.findViewById(R.id.editVaccines);
        changeProfilePicButton = view.findViewById(R.id.changeProfilePicButton);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton); // הוספת התייחסות לכפתור ביטול

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        vetId = sharedPreferences.getString("vetId", ""); // Optional pre-assignment

        changeProfilePicButton.setOnClickListener(v -> showImagePickerDialog());
        saveButton.setOnClickListener(v -> saveDogProfile());

        // הוספת מאזין אירועים לכפתור הביטול
        cancelButton.setOnClickListener(v -> navigateBack());

        return view;
    }

    private void saveDogProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();
        String race = editRace.getText().toString().trim();
        String allergies = editAllergies.getText().toString().trim();
        String vaccines = editVaccines.getText().toString().trim();

        if (name.isEmpty() || race.isEmpty() || birthday.isEmpty() || weight.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String age = calculateDogAge(birthday);
        // יצירת מזהה ייחודי לכלב
        dogId = UUID.randomUUID().toString();
        String bio = buildBio(race, weight, allergies, vaccines);
        String ownerId = currentUser.getUid();

        Log.d(TAG, "Saving new dog profile: " + name + ", ID: " + dogId + ", Owner: " + ownerId);

        // שליחת המידע לפיירסטור
        FirestoreUserHelper.addDogProfile(
                dogId, name, race, Integer.parseInt(age), birthday, weight, allergies, vaccines,
                bio, ownerId, vetId, null
        );

        if (selectedImageUri != null) {
            String dogId = "הערך של ה-dogId כאן"; // שלוף את ה-dogId שלך כאן (לפי איך שהוא מאוחסן או נשלח)
            uploadImageToFirebase(selectedImageUri, ownerId, dogId);
        }


        // יצירת אובייקט DogProfile
        DogProfile newDogProfile = new DogProfile(
                dogId, name, age, bio,
                selectedImageUri != null ? selectedImageUri.toString() : "",
                race, birthday, weight, allergies, vaccines, ownerId, vetId
        );

        // שליחת המידע ל-HomeFragment כדי לעדכן את הפרופיל העליון
        Bundle result = new Bundle();
        result.putString("updatedBio", bio);
        result.putString("updatedDogAge", age);
        result.putString("updatedUserName", name);

        // שמירת שאר הפרטים
        result.putString("race", race);
        result.putString("birthday", birthday);
        result.putString("weight", weight);
        result.putString("allergies", allergies);
        result.putString("vaccines", vaccines);
        result.putString("dogId", dogId);

        // שליחת קישור התמונה ב-Bundle
        if (selectedImageUri != null) {
            result.putString("updatedImageUri", selectedImageUri.toString());
        }

        // שליחת התוצאה ל-HomeFragment
        getParentFragmentManager().setFragmentResult("editProfileKey", result);

        // חזרה למסך הבית
        Toast.makeText(getContext(), "Dog profile added", Toast.LENGTH_SHORT).show();
        navigateBack();
    }

    // פונקציה חדשה לחזרה למסך הקודם
    private void navigateBack() {
        // אפשרות 1: באמצעות Fragment Manager
        requireActivity().getSupportFragmentManager().popBackStack();

        // אפשרות 2: באמצעות Navigation Component (אם הוא בשימוש)
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigateUp();
        } catch (Exception e) {
            // אם Navigation לא זמין, נשתמש באפשרות 1
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private String calculateDogAge(String birthday) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date birthDate = format.parse(birthday);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--;
            return String.valueOf(age);
        } catch (ParseException e) {
            return "0";
        }
    }

    private String buildBio(String race, String weight, String allergies, String vaccines) {
        StringBuilder bioBuilder = new StringBuilder();

        // הגזע יוצג בנפרד, לא בביו
        if (!weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(weight).append(" kg\n");
        }
        if (!allergies.isEmpty()) {
            bioBuilder.append("Allergies: ").append(allergies).append("\n");
        }
        if (!vaccines.isEmpty()) {
            bioBuilder.append("Vaccines: ").append(vaccines);
        }

        return bioBuilder.toString().trim();
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else if (which == 1) {
                        takePhoto();
                    }
                })
                .show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                selectedImageUri = data.getData();
                editProfilePic.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                selectedImageUri = getImageUriFromBitmap(bitmap);
                editProfilePic.setImageURI(selectedImageUri);
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageData = baos.toByteArray();
        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase(Uri imageUri, String ownerId, String dogId) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("profile_pics/" + ownerId + "/" + UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Image uploaded successfully: " + uri.toString());

                // עכשיו נעשה עדכון של ה-URL ב-Firestore תחת DogProfiles
                FirestoreUserHelper.uploadDogProfileImage(imageUri, dogId, ownerId, new FirestoreUserHelper.OnImageUploadListener() {
                    @Override
                    public void onUploadSuccess(String imageUrl) {
                        // כאן אפשר להוסיף עוד לוגיקה אחרי העלאת התמונה והעדכון
                        Log.d(TAG, "Image URL successfully updated in Firestore: " + imageUrl);
                    }

                    @Override
                    public void onUploadFailed(Exception exception) {
                        // במקרה של כישלון
                        Log.e(TAG, "Failed to update image URL in Firestore: " + exception.getMessage());
                    }
                });
            });
        }).addOnFailureListener(e -> Log.e(TAG, "Image upload failed", e));
    }

}
