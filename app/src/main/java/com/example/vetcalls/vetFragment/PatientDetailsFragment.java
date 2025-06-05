package com.example.vetcalls.vetFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.Appointment;
import com.example.vetcalls.obj.DogProfile;
import com.example.vetcalls.obj.DogProfileAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Fragment for displaying patient (dog) details and appointment history for veterinarians.
 * Provides a list view of all dogs assigned to the current veterinarian and detailed view for individual dogs.
 * Handles navigation between list and detail views, and displays appointment history.
 *
 * @author Ofek Levi
 */
public class PatientDetailsFragment extends Fragment {

    /** TextView components for displaying dog information */
    private TextView nameText, birthdayText, raceText, weightText, vaccinesText, allergiesText, lastVisitText;

    /** ImageView for displaying the dog's profile picture */
    private ImageView dogImage;

    /** Container layout for displaying appointment buttons */
    private LinearLayout appointmentsContainer;

    /** Container layout for dog details display */
    private LinearLayout detailsContainer;

    /** RecyclerView for displaying the list of dogs */
    private RecyclerView dogsRecyclerView;

    /** Firebase Firestore database instance */
    private FirebaseFirestore db;

    /** Owner ID of the currently selected dog */
    private String ownerId;

    /** Current veterinarian's unique identifier */
    private String vetId;

    /** Flag indicating whether to show list view or detail view */
    private boolean showList = true;

    /** List containing all dog profiles assigned to this veterinarian */
    private List<DogProfile> dogList = new ArrayList<>();

    /** Adapter for managing dog profiles in the RecyclerView */
    private DogProfileAdapter dogAdapter;

    /** Button for navigating back to the dog list */
    private Button backToListButton;

    /** Reference to the last selected dog for state restoration */
    private DogProfile lastSelectedDog = null;

    /**
     * Creates and initializes the fragment view with all UI components and data loading.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment's UI will be attached to
     * @param savedInstanceState Bundle containing the fragment's previously saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_details, container, false);

        initializeViews(view);
        setupRecyclerView();
        initializeFirebase();
        setupInitialState();
        setupBackButton(view);

        return view;
    }

    /**
     * Initializes all UI components from the layout.
     *
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        nameText = view.findViewById(R.id.dogName);
        birthdayText = view.findViewById(R.id.patientBirthday);
        raceText = view.findViewById(R.id.patientRace);
        weightText = view.findViewById(R.id.patientWeight);
        vaccinesText = view.findViewById(R.id.patientVaccines);
        allergiesText = view.findViewById(R.id.patientAllergies);
        lastVisitText = view.findViewById(R.id.lastVisitText);
        dogImage = view.findViewById(R.id.dogImage);
        appointmentsContainer = view.findViewById(R.id.appointmentsContainer);
        detailsContainer = view.findViewById(R.id.detailsContainer);
        dogsRecyclerView = view.findViewById(R.id.dogsRecyclerView);
    }

    /**
     * Sets up the RecyclerView with layout manager and adapter.
     */
    private void setupRecyclerView() {
        dogsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dogAdapter = new DogProfileAdapter(getContext(), dogList, position -> showDogDetails(dogList.get(position)), 0);
        dogsRecyclerView.setAdapter(dogAdapter);
    }

    /**
     * Initializes Firebase services and gets current veterinarian ID.
     */
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        vetId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    /**
     * Sets up the initial state of the fragment based on showList flag.
     */
    private void setupInitialState() {
        if (showList) {
            loadVetDogs();
            detailsContainer.setVisibility(View.GONE);
            dogsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            detailsContainer.setVisibility(View.VISIBLE);
            dogsRecyclerView.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the back to list button with click listener.
     *
     * @param view The root view of the fragment
     */
    private void setupBackButton(View view) {
        backToListButton = view.findViewById(R.id.backToListButton);
        backToListButton.setOnClickListener(v -> showDogList());
    }

    /**
     * Loads all dogs assigned to the current veterinarian from Firestore.
     * Populates the dog list and updates the RecyclerView adapter.
     */
    private void loadVetDogs() {
        db.collection("DogProfiles")
                .whereEqualTo("vetId", vetId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    dogList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DogProfile dog = createDogProfileFromDocument(doc);
                        dogList.add(dog);
                    }
                    dogAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Creates a DogProfile object from a Firestore document.
     *
     * @param doc The Firestore document containing dog data
     * @return DogProfile object populated with document data
     */
    private DogProfile createDogProfileFromDocument(DocumentSnapshot doc) {
        DogProfile dog = new DogProfile();
        dog.dogId = doc.getId();
        dog.name = getStringField(doc, "name");

        Object ageObj = doc.get("age");
        if (ageObj instanceof Long) {
            dog.age = String.valueOf(ageObj);
        } else if (ageObj != null) {
            dog.age = ageObj.toString();
        } else {
            dog.age = "Unknown";
        }

        dog.bio = getStringField(doc, "bio");
        dog.profileImageUrl = getStringField(doc, "profileImageUrl");
        dog.race = getStringField(doc, "race");
        dog.birthday = getStringField(doc, "birthday");
        dog.weight = getStringField(doc, "weight");
        dog.allergies = getStringField(doc, "allergies");
        dog.vaccines = getStringField(doc, "vaccines");
        dog.ownerId = getStringField(doc, "ownerId");
        dog.vetId = getStringField(doc, "vetId");
        dog.vetName = getStringField(doc, "vetName");

        return dog;
    }

    /**
     * Safely extracts a string field from a Firestore document.
     *
     * @param doc The Firestore document
     * @param field The field name to extract
     * @return The field value as string, or empty string if null
     */
    private String getStringField(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value != null ? value.toString() : "";
    }

    /**
     * Displays detailed information for the selected dog and loads appointment history.
     * Switches from list view to detail view and populates all dog information fields.
     *
     * @param dog The DogProfile object to display details for
     */
    private void showDogDetails(DogProfile dog) {
        showList = false;
        lastSelectedDog = dog;

        populateDogInformation(dog);
        loadDogImage(dog);
        switchToDetailView();
        loadAppointmentHistoryForDog(dog.dogId, dog.name, dog.vetId);
    }

    /**
     * Populates the UI fields with the dog's information.
     *
     * @param dog The DogProfile object containing the dog's data
     */
    private void populateDogInformation(DogProfile dog) {
        nameText.setText("Name: " + dog.name);
        birthdayText.setText("Birthday: " + dog.birthday);
        raceText.setText("Race: " + dog.race);
        weightText.setText("Weight: " + dog.weight + " kg");
        vaccinesText.setText("Vaccines: " + (dog.vaccines != null && !dog.vaccines.isEmpty() ? dog.vaccines : "None"));
        allergiesText.setText("Allergies: " + (dog.allergies != null && !dog.allergies.isEmpty() ? dog.allergies : "None"));
    }

    /**
     * Loads the dog's profile image using Glide.
     *
     * @param dog The DogProfile object containing the image URL
     */
    private void loadDogImage(DogProfile dog) {
        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(dog.profileImageUrl)
                    .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                    .into(dogImage);
        }
    }

    /**
     * Switches the UI from list view to detail view.
     */
    private void switchToDetailView() {
        detailsContainer.setVisibility(View.VISIBLE);
        dogsRecyclerView.setVisibility(View.GONE);
    }

    /**
     * Loads and displays appointment history for the specified dog.
     * Creates clickable buttons for each appointment that navigate to appointment details.
     *
     * @param dogId The unique identifier of the dog
     * @param dogName The name of the dog
     * @param vetId The veterinarian's unique identifier
     */
    private void loadAppointmentHistoryForDog(String dogId, String dogName, String vetId) {
        db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    appointmentsContainer.removeAllViews();
                    boolean hasHistory = false;

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        Button appointmentButton = createAppointmentButton(doc, dogId, dogName, vetId);
                        appointmentsContainer.addView(appointmentButton);
                        hasHistory = true;
                    }

                    if (!hasHistory) {
                        showNoAppointmentsMessage();
                    }
                });
    }

    /**
     * Creates a clickable button for an appointment with navigation to appointment details.
     *
     * @param doc The Firestore document containing appointment data
     * @param dogId The unique identifier of the dog
     * @param dogName The name of the dog
     * @param vetId The veterinarian's unique identifier
     * @return Button configured with appointment information and click listener
     */
    private Button createAppointmentButton(QueryDocumentSnapshot doc, String dogId, String dogName, String vetId) {
        String date = doc.getString("date");
        String endTime = doc.getString("endTime");
        String type = doc.getString("type");
        String notes = doc.getString("notes");
        String appointmentId = doc.getString("id");
        String vetName = doc.getString("vetName");
        String ownerId = doc.getString("ownerId");
        String startTime = doc.getString("startTime");

        Button appointmentButton = new Button(getContext());
        appointmentButton.setText((date != null ? date : "") + " " + (startTime != null ? startTime : "") + " - " + (type != null ? type : ""));

        appointmentButton.setOnClickListener(v -> {
            Bundle args = createAppointmentDetailsArgs(appointmentId, date, startTime, notes, vetName, type, dogId, vetId, dogName);
            navigateToAppointmentDetails(args);
        });

        return appointmentButton;
    }

    /**
     * Creates arguments bundle for appointment details fragment.
     *
     * @param appointmentId The unique identifier of the appointment
     * @param date The appointment date
     * @param startTime The appointment start time
     * @param notes The appointment notes
     * @param vetName The veterinarian's name
     * @param type The appointment type
     * @param dogId The unique identifier of the dog
     * @param vetId The veterinarian's unique identifier
     * @param dogName The name of the dog
     * @return Bundle containing all appointment details
     */
    private Bundle createAppointmentDetailsArgs(String appointmentId, String date, String startTime, String notes, String vetName, String type, String dogId, String vetId, String dogName) {
        Bundle args = new Bundle();
        args.putString("appointmentId", appointmentId);
        args.putString("date", date);
        args.putString("time", startTime);
        args.putString("details", notes);
        args.putString("veterinarian", vetName);
        args.putString("type", type);
        args.putString("dogId", dogId);
        args.putString("vetId", vetId);
        args.putString("dogName", dogName);
        args.putBoolean("showActions", false);
        return args;
    }

    /**
     * Navigates to the appointment details fragment with the provided arguments.
     *
     * @param args Bundle containing appointment details for the target fragment
     */
    private void navigateToAppointmentDetails(Bundle args) {
        com.example.vetcalls.usersFragment.AppointmentDetailsFragment detailsFragment = new com.example.vetcalls.usersFragment.AppointmentDetailsFragment();
        detailsFragment.setArguments(args);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Displays a message when no appointments are found for the dog.
     */
    private void showNoAppointmentsMessage() {
        TextView noHistory = new TextView(getContext());
        noHistory.setText("No appointments found");
        appointmentsContainer.addView(noHistory);
    }

    /**
     * Switches the view back to the dog list from detail view.
     */
    private void showDogList() {
        showList = true;
        detailsContainer.setVisibility(View.GONE);
        dogsRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Called when the fragment becomes visible to the user again.
     * Restores the detail view if a dog was previously selected.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (!showList && lastSelectedDog != null) {
            showDogDetails(lastSelectedDog);
        }
    }
}