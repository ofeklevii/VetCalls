package com.example.vetcalls.usersFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.obj.Veterinarian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment for editing or creating a dog's profile.
 * This fragment allows users to input and modify details about their dog,
 * including name, birthday, weight, race, allergies, vaccines, and profile picture.
 * It also handles veterinarian selection and image uploading to Firebase.
 *
 * @author Ofek Levi
 */
public class EditProfileFragment extends Fragment {
    /**
     * Log tag for this fragment.
     */
    private static final String TAG = "EditProfileFragment";

    /** EditText for the dog's name. */
    private EditText editName;
    /** EditText for the dog's birthday. */
    private EditText editBirthday;
    /** EditText for the dog's weight. */
    private EditText editWeight;
    /** EditText for the dog's race. */
    private EditText editRace;
    /** EditText for the dog's allergies. */
    private EditText editAllergies;
    /** EditText for the dog's vaccines. */
    private EditText editVaccines;

    /** Button to save the profile changes. */
    private Button saveButton;
    /** Button to trigger the profile picture change dialog. */
    private Button changeProfilePicButton;
    /** Button to cancel the editing process and navigate back. */
    private Button cancelButton;

    /** ImageView to display and edit the dog's profile picture. */
    private ImageView editProfilePic;

    /** SharedPreferences for storing and retrieving user profile data locally. */
    private SharedPreferences sharedPreferences;

    /** Firebase Firestore instance for database operations. */
    private FirebaseFirestore db;
    /** FirebaseAuth instance for user authentication. */
    private FirebaseAuth auth;

    /** URI of the image selected by the user for the profile picture. */
    private Uri selectedImageUri;

    /** Request code for picking an image from the gallery. */
    private static final int REQUEST_IMAGE_PICK = 1;
    /** Request code for capturing an image using the camera. */
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    /** ID of the dog currently being edited. If empty or null, a new dog profile is being created. */
    private String dogId;
    /** Flag indicating whether a new dog profile is being created (true) or an existing one is being edited (false). */
    private boolean isNewDog = false;
    /** Download URL of the profile image stored in Firebase Storage. */
    private String downloadUrl = null;

    /** Spinner for selecting the dog's veterinarian. */
    private Spinner vetSpinner;
    /** List of veterinarian names to populate the vetSpinner. */
    private List<String> vetNames = new ArrayList<>();
    /** Map associating veterinarian names with their corresponding Firebase IDs. */
    private Map<String, String> vetNameToId = new HashMap<>();
    /** ID of the veterinarian selected in the vetSpinner. */
    private String selectedVetId = null;
    /** Name of the veterinarian selected in the vetSpinner. */
    private String selectedVetName = null;
    /** Timestamp (in milliseconds) of the last time the veterinarian was changed for this dog. Loaded from Firestore. */
    private long lastVetChange = 0L;
    /** The original veterinarian ID when the profile was loaded, used to detect if the vet has been changed. */
    private String originalVetId = null;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onViewCreated(View, Bundle)}.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
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
        cancelButton = view.findViewById(R.id.cancelButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        vetSpinner = view.findViewById(R.id.vetSpinner);
        loadVetList();

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        clearAllFields();

        if (getArguments() != null) {
            dogId = getArguments().getString("dogId", "");
            String imageUrl = getArguments().getString("imageUrl", "");
            Log.d(TAG, "Received dogId from arguments: " + dogId);
            Log.d(TAG, "Received imageUrl from arguments: " + imageUrl);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadUrl = imageUrl;
                loadProfileImage(editProfilePic, imageUrl);
            }
        } else {
            dogId = sharedPreferences.getString("dogId", "");
            Log.d(TAG, "Using dogId from SharedPreferences: " + dogId);

            String savedImageUrl = sharedPreferences.getString("profileImageUrl", null);
            if (savedImageUrl == null || savedImageUrl.isEmpty()) {
                savedImageUrl = sharedPreferences.getString("imageUrl", null);
            }
            if (savedImageUrl != null && !savedImageUrl.isEmpty()) {
                downloadUrl = savedImageUrl;
                loadProfileImage(editProfilePic, savedImageUrl);
            }
        }

        isNewDog = (dogId == null || dogId.isEmpty());
        Log.d(TAG, "Is this a new dog? " + isNewDog);

        if (!isNewDog) {
            loadDogDataFromFirestore(dogId);
        }

        saveButton.setOnClickListener(v -> saveProfile());
        changeProfilePicButton.setOnClickListener(v -> showImagePickerDialog());
        cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    /**
     * Loads the list of veterinarians from the "Veterinarians" collection in Firestore
     * and populates the vetSpinner with their names.
     * It also restores the previously selected veterinarian if available.
     */
    private void loadVetList() {
        db.collection("Veterinarians")
                .get()
                .addOnSuccessListener(query -> {
                    vetNames.clear();
                    vetNameToId.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Veterinarian vet = doc.toObject(Veterinarian.class);
                        String name = vet != null ? vet.fullName : null;
                        String id = doc.getId();
                        if (name != null) {
                            vetNames.add(name);
                            vetNameToId.put(name, id);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vetNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    vetSpinner.setAdapter(adapter);

                    String savedVetName = sharedPreferences.getString("vetName", null);
                    if (savedVetName != null && vetNames.contains(savedVetName)) {
                        vetSpinner.setSelection(vetNames.indexOf(savedVetName));
                    }

                    vetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedVetName = vetNames.get(position);
                            selectedVetId = vetNameToId.get(selectedVetName);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedVetName = null;
                            selectedVetId = null;
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading vet list: " + e.getMessage()));
    }

    /**
     * Loads an image from the given URL into the specified ImageView using Glide.
     * Displays a placeholder if the URL is invalid or loading fails.
     *
     * @param imageView The ImageView to load the image into.
     * @param imageUrl The URL of the image to load. Can be a web URL or a local file URI.
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
     * Clears all input fields (name, birthday, weight, etc.) and resets the
     * profile picture ImageView to a default placeholder.
     */
    private void clearAllFields() {
        editName.setText("");
        editBirthday.setText("");
        editWeight.setText("");
        editRace.setText("");
        editAllergies.setText("");
        editVaccines.setText("");
        editProfilePic.setImageResource(R.drawable.user_person_profile_avatar_icon_190943);
    }

    /**
     * Loads dog data from the "DogProfiles" collection in Firestore using the provided dogId.
     * Populates the input fields with the retrieved data, including the profile image and veterinarian.
     *
     * @param dogId The unique identifier of the dog whose data needs to be loaded.
     */
    private void loadDogDataFromFirestore(String dogId) {
        Log.d(TAG, "Loading dog data for id: " + dogId);
        if (dogId == null || dogId.isEmpty()) {
            return;
        }
        db.collection("DogProfiles").document(dogId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Dog document exists");
                        String name = documentSnapshot.getString("name");
                        String birthday = documentSnapshot.getString("birthday");
                        String weight = extractStringOrNumber(documentSnapshot, "weight", "");
                        String race = documentSnapshot.getString("race");
                        String allergies = documentSnapshot.getString("allergies");
                        String vaccines = documentSnapshot.getString("vaccines");
                        String vetId = documentSnapshot.getString("vetId");
                        String vetName = documentSnapshot.getString("vetName");
                        String age = extractStringOrNumber(documentSnapshot, "age", "0"); // This field was used for lastVetChange
                        Long firestoreLastVetChange = documentSnapshot.getLong("lastVetChange");

                        editName.setText(name);
                        editBirthday.setText(birthday);
                        editWeight.setText(weight);
                        editRace.setText(race);
                        editAllergies.setText(allergies);
                        editVaccines.setText(vaccines);

                        selectedVetId = vetId;
                        selectedVetName = vetName;
                        int position = vetNames.indexOf(vetName);
                        if (position != -1) {
                            vetSpinner.setSelection(position);
                        }

                        if (firestoreLastVetChange != null) {
                            lastVetChange = firestoreLastVetChange;
                        } else {
                            try {
                                lastVetChange = Long.parseLong(age);
                            } catch (NumberFormatException e) {
                                lastVetChange = 0L;
                                Log.w(TAG, "Could not parse 'age' as Long for lastVetChange, defaulting to 0.");
                            }
                        }
                        Log.d(TAG, "Loaded lastVetChange from Firestore: " + lastVetChange + " for dog: " + dogId);

                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        Log.d(TAG, "Loaded image URL: " + imageUrl);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            loadProfileImage(editProfilePic, imageUrl);
                            downloadUrl = imageUrl;
                        }
                        Log.d(TAG, "Dog data loaded successfully");
                        originalVetId = vetId;
                    } else {
                        Log.d(TAG, "Dog document doesn't exist");
                        lastVetChange = 0L;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog data: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading dog data", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Gathers data from input fields, validates it, and saves the dog's profile.
     * If it's a new dog, a new ID is generated.
     * Handles image uploading if a new image was selected.
     * Updates data in Firestore and SharedPreferences.
     * Restricts veterinarian changes to once per week for existing dogs.
     */
    private void saveProfile() {
        Log.d(TAG, "Starting to save profile...");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String race = editRace.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();
        String allergies = editAllergies.getText().toString().trim();
        String vaccines = editVaccines.getText().toString().trim();
        String ownerId = currentUser.getUid();

        if (name.isEmpty() || weight.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields (name and weight)", Toast.LENGTH_SHORT).show();
            return;
        }

        String ageStr = calculateDogAge(birthday);
        int age = 0;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Could not parse age as integer: " + ageStr);
        }

        if (dogId == null || dogId.isEmpty()) {
            dogId = UUID.randomUUID().toString();
            isNewDog = true;
            Log.d(TAG, "This is a new dog with ID: " + dogId);
        } else {
            Log.d(TAG, "Updating existing dog ID: " + dogId);
        }

        String bio = buildBio(weight, allergies, vaccines, race, birthday);

        if (selectedVetId == null || selectedVetName == null) {
            Toast.makeText(requireContext(), "Please select a vet to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        long oneWeekMillis = 7 * 24 * 60 * 60 * 1000;
        boolean vetChanged = originalVetId != null && !originalVetId.equals(selectedVetId);

        DogProfile dogProfile = new DogProfile();
        dogProfile.dogId = dogId;
        dogProfile.name = name;
        dogProfile.vetId = selectedVetId;
        dogProfile.vetName = selectedVetName;
        dogProfile.weight = weight;
        dogProfile.allergies = allergies;
        dogProfile.vaccines = vaccines;
        dogProfile.bio = bio;
        dogProfile.ownerId = ownerId;
        dogProfile.lastUpdated = System.currentTimeMillis();
        dogProfile.race = race;
        dogProfile.birthday = birthday;
        dogProfile.age = ageStr;

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            dogProfile.profileImageUrl = downloadUrl;
        } else {
            dogProfile.profileImageUrl = "";
        }

        if (!isNewDog && vetChanged) {
            if (lastVetChange != 0 && (now - lastVetChange) < oneWeekMillis) {
                Log.d(TAG, "Vet change rejected - Less than a week since last change");
                Toast.makeText(requireContext(), "Vet can only be changed once a week", Toast.LENGTH_SHORT).show();
                if (originalVetId != null && vetNameToId.containsValue(originalVetId)) {
                    for (Map.Entry<String, String> entry : vetNameToId.entrySet()) {
                        if (entry.getValue().equals(originalVetId)) {
                            selectedVetName = entry.getKey();
                            selectedVetId = originalVetId;
                            int originalPos = vetNames.indexOf(selectedVetName);
                            if (originalPos != -1) {
                                vetSpinner.setSelection(originalPos);
                            }
                            break;
                        }
                    }
                }
                return;
            }
            dogProfile.lastVetChange = now;
            Log.d(TAG, "Vet change allowed - Proceeding with save. New lastVetChange: " + now);
        } else if (isNewDog) {
            dogProfile.lastVetChange = now;
            Log.d(TAG, "New dog - Setting initial lastVetChange: " + now);
        } else {
            dogProfile.lastVetChange = (lastVetChange > 0) ? lastVetChange : now;
            Log.d(TAG, "Vet not changed or initial setup - lastVetChange: " + dogProfile.lastVetChange);
        }


        Log.d(TAG, "Saving profile with dogId: " + dogId);

        FirestoreUserHelper.updateDogProfileEverywhere(dogProfile);

        db.collection("Users").document(ownerId)
                .collection("Dogs").document(dogId)
                .set(dogProfile)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dog full data saved to user's Dogs subcollection"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving dog to user's Dogs subcollection: " + e.getMessage()));

        if (selectedImageUri != null) {
            uploadImageToFirebase(selectedImageUri);
        } else {
            finishSaveProcess(name, ageStr, bio, race, birthday, weight, allergies, vaccines);
        }
    }

    /**
     * Constructs a biography string for the dog based on its details.
     *
     * @param weight The dog's weight.
     * @param allergies The dog's allergies.
     * @param vaccines The dog's vaccination status.
     * @param race The dog's race.
     * @param birthday The dog's birthday.
     * @return A formatted string containing the dog's biography.
     */
    private String buildBio(String weight, String allergies, String vaccines, String race, String birthday) {
        StringBuilder bioBuilder = new StringBuilder();
        if (weight != null && !weight.isEmpty()) {
            bioBuilder.append("Weight: ").append(weight).append(" kg");
        }
        if (race != null && !race.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Race: ").append(race);
        }
        if (birthday != null && !birthday.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Birthday: ").append(birthday);
        }
        if (allergies != null && !allergies.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Allergies: ").append(allergies);
        }
        if (vaccines != null && !vaccines.isEmpty()) {
            if (bioBuilder.length() > 0) bioBuilder.append("\n");
            bioBuilder.append("Vaccines: ").append(vaccines);
        }
        return bioBuilder.toString().trim();
    }

    /**
     * Finalizes the profile saving process. This includes:
     * - Saving profile data to SharedPreferences.
     * - Preparing a result Bundle with updated data for the parent fragment.
     * - Setting the fragment result.
     * - Displaying a success toast.
     * - Navigating back to the previous screen after a short delay.
     *
     * @param name The dog's name.
     * @param age The dog's calculated age.
     * @param bio The dog's constructed biography.
     * @param race The dog's race.
     * @param birthday The dog's birthday.
     * @param weight The dog's weight.
     * @param allergies The dog's allergies.
     * @param vaccines The dog's vaccination status.
     */
    private void finishSaveProcess(String name, String age, String bio, String race,
                                   String birthday, String weight, String allergies, String vaccines) {
        Log.d(TAG, "Finishing save process");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("birthday", birthday);
        editor.putString("weight", weight);
        editor.putString("race", race);
        editor.putString("allergies", allergies);
        editor.putString("vaccines", vaccines);
        editor.putString("age", age);
        editor.putString("vetId", selectedVetId);
        editor.putString("vetName", selectedVetName);
        editor.putString("bio", bio);
        editor.putString("dogId", dogId);

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            editor.putString("profileImageUrl", downloadUrl);
            Log.d(TAG, "Saving image URL to SharedPreferences: " + downloadUrl);
        } else {
            editor.remove("profileImageUrl");
        }

        editor.apply();

        Log.d(TAG, "Saved to SharedPreferences, dogId: " + dogId);

        Bundle result = new Bundle();
        result.putString("updatedBio", bio);
        result.putString("updatedDogAge", age);
        result.putString("updatedUserName", name);
        result.putString("race", race);
        result.putString("birthday", birthday);
        result.putString("weight", weight);
        result.putString("allergies", allergies);
        result.putString("vaccines", vaccines);
        result.putString("dogId", dogId);
        result.putString("vetId", selectedVetId);
        result.putString("vetName", selectedVetName);
        result.putBoolean("isNewDog", isNewDog);

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            result.putString("updatedImageUri", downloadUrl);
            Log.d(TAG, "Sending image URL in result: " + downloadUrl);
        } else if (selectedImageUri != null) {
            result.putString("updatedImageUri", selectedImageUri.toString());
            Log.d(TAG, "Sending selected image URI in result: " + selectedImageUri);
        }

        Log.d(TAG, "Setting fragment result with dogId: " + dogId);
        getParentFragmentManager().setFragmentResult("editProfileKey", result);

        Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Finishing profile save and returning to HomeFragment");

        new android.os.Handler().postDelayed(() -> {
            if (isAdded()) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        }, 500);
    }

    /**
     * Calculates the dog's age in years based on its birthday.
     *
     * @param birthday The dog's birthday string in "yyyy-MM-dd" format.
     * @return The calculated age as a string. Returns "0" if the birthday is invalid or cannot be parsed.
     */
    private String calculateDogAge(String birthday) {
        if (birthday == null || birthday.isEmpty()) return "0";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date birthDate = dateFormat.parse(birthday);
            if (birthDate == null) return "0";

            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return String.valueOf(Math.max(0, age));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing birthday: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Displays an AlertDialog offering the user options to select a profile picture:
     * either by choosing from the gallery or taking a new photo with the camera.
     */
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

    /**
     * Initiates an Intent to allow the user to pick an image from the device's gallery.
     * The result is handled in {@link #onActivityResult(int, int, Intent)}.
     */
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    /**
     * Initiates an Intent to allow the user to capture a new photo using the device's camera.
     * The result is handled in {@link #onActivityResult(int, int, Intent)}.
     */
    private void takePhotoWithCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Handles the result from started activities, specifically for image picking or capturing.
     * If an image is successfully selected or captured, its URI is stored,
     * and the profile picture ImageView is updated.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                selectedImageUri = data.getData();
                Log.d(TAG, "Image selected from gallery: " + selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    selectedImageUri = getImageUriFromBitmap(requireContext(), photo);
                    Log.d(TAG, "Image captured from camera: " + selectedImageUri);
                }
            }

            if (selectedImageUri != null) {
                loadProfileImage(editProfilePic, selectedImageUri.toString());
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Converts a Bitmap image to a content URI that can be used by other apps or for uploading.
     * The image is saved to the device's MediaStore.
     *
     * @param context The context used to access the ContentResolver.
     * @param bitmap The Bitmap image to convert.
     * @return The content URI of the saved image, or null if the conversion fails.
     */
    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "DogProfile_" + System.currentTimeMillis(), null);
        return path != null ? Uri.parse(path) : null;
    }

    /**
     * Uploads the specified image URI to Firebase Storage under the "dog_profile_images" path.
     * If successful, it retrieves the download URL, updates Firestore with this URL,
     * and then proceeds to {@link #finishSaveProcess(String, String, String, String, String, String, String, String)}.
     * If no image URI is provided, it directly calls finishSaveProcess.
     * Displays a loading dialog during the upload.
     *
     * @param imageUri The URI of the image to be uploaded. If null, the method proceeds without uploading.
     */
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            finishSaveProcess(editName.getText().toString().trim(),
                    calculateDogAge(editBirthday.getText().toString().trim()),
                    buildBio(editWeight.getText().toString().trim(),
                            editAllergies.getText().toString().trim(),
                            editVaccines.getText().toString().trim(),
                            editRace.getText().toString().trim(),
                            editBirthday.getText().toString().trim()),
                    editRace.getText().toString().trim(),
                    editBirthday.getText().toString().trim(),
                    editWeight.getText().toString().trim(),
                    editAllergies.getText().toString().trim(),
                    editVaccines.getText().toString().trim());
            return;
        }

        Log.d(TAG, "Uploading image to Firebase Storage, URI: " + imageUri);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("dog_profile_images")
                .child(dogId + ".jpg");

        Log.d(TAG, "Storage reference path: " + storageRef.getPath());

        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Uploading image...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");

                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        downloadUrl = uri.toString();
                        Log.d(TAG, "Got download URL: " + downloadUrl);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", downloadUrl);

                        db.collection("DogProfiles")
                                .document(dogId)
                                .update(updates) // Changed from set(updates, SetOptions.merge()) to update() for clarity
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Image URLs updated in Firestore");
                                    if (loadingDialog.isShowing()) loadingDialog.dismiss();

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("profileImageUrl", downloadUrl);
                                    editor.apply();

                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim(), editRace.getText().toString().trim(), editBirthday.getText().toString().trim()),
                                            editRace.getText().toString().trim(), editBirthday.getText().toString().trim(), editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating image URLs in Firestore: " + e.getMessage());
                                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                                    finishSaveProcess(editName.getText().toString().trim(),
                                            calculateDogAge(editBirthday.getText().toString().trim()),
                                            buildBio(editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim(), editRace.getText().toString().trim(), editBirthday.getText().toString().trim()),
                                            editRace.getText().toString().trim(), editBirthday.getText().toString().trim(), editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim());
                                });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                        if (loadingDialog.isShowing()) loadingDialog.dismiss();
                        finishSaveProcess(editName.getText().toString().trim(),
                                calculateDogAge(editBirthday.getText().toString().trim()),
                                buildBio(editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim(), editRace.getText().toString().trim(), editBirthday.getText().toString().trim()),
                                editRace.getText().toString().trim(), editBirthday.getText().toString().trim(), editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                    Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finishSaveProcess(editName.getText().toString().trim(),
                            calculateDogAge(editBirthday.getText().toString().trim()),
                            buildBio(editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim(), editRace.getText().toString().trim(), editBirthday.getText().toString().trim()),
                            editRace.getText().toString().trim(), editBirthday.getText().toString().trim(), editWeight.getText().toString().trim(), editAllergies.getText().toString().trim(), editVaccines.getText().toString().trim());
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
    }

    /**
     * Safely extracts a field value from a Firestore DocumentSnapshot, attempting to convert
     * it to a String. This handles cases where the field might be stored as a String or a Number.
     *
     * @param document The DocumentSnapshot from which to extract the field.
     * @param field The name of the field to extract.
     * @param defaultValue The default value to return if the field is not found or cannot be converted.
     * @return The field value as a String, or the defaultValue if an issue occurs.
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
}