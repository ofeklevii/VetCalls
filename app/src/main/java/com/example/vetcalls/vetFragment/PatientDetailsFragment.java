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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class PatientDetailsFragment extends Fragment {

    private TextView nameText, birthdayText, raceText, weightText, vaccinesText, allergiesText, lastVisitText;
    private ImageView dogImage;
    private LinearLayout appointmentsContainer;
    private LinearLayout detailsContainer;
    private RecyclerView dogsRecyclerView;
    private FirebaseFirestore db;

    private String ownerId;
    private String vetId;
    private boolean showList = true;
    private List<DogProfile> dogList = new ArrayList<>();
    private DogProfileAdapter dogAdapter;
    private Button backToListButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_details, container, false);

        // Initialize views
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
        dogsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dogAdapter = new DogProfileAdapter(getContext(), dogList, position -> showDogDetails(dogList.get(position)), 0);
        dogsRecyclerView.setAdapter(dogAdapter);

        db = FirebaseFirestore.getInstance();

        vetId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (showList) {
            loadVetDogs();
            detailsContainer.setVisibility(View.GONE);
            dogsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            detailsContainer.setVisibility(View.VISIBLE);
            dogsRecyclerView.setVisibility(View.GONE);
        }

        backToListButton = view.findViewById(R.id.backToListButton);
        backToListButton.setOnClickListener(v -> showDogList());

        return view;
    }

    private void loadVetDogs() {
        db.collection("DogProfiles")
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                dogList.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
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
                    dogList.add(dog);
                }
                dogAdapter.notifyDataSetChanged();
            });
    }

    private String getStringField(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value != null ? value.toString() : "";
    }

    private void showDogDetails(DogProfile dog) {
        showList = false;
        nameText.setText("Name: " + dog.name);
        birthdayText.setText("Birthday: " + dog.birthday);
        raceText.setText("Race: " + dog.race);
        weightText.setText("Weight: " + dog.weight + " kg");
        vaccinesText.setText("Vaccines: " + (dog.vaccines != null && !dog.vaccines.isEmpty() ? dog.vaccines : "None"));
        allergiesText.setText("Allergies: " + (dog.allergies != null && !dog.allergies.isEmpty() ? dog.allergies : "None"));
        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            Glide.with(this)
                .load(dog.profileImageUrl)
                .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                .into(dogImage);
        }
        detailsContainer.setVisibility(View.VISIBLE);
        dogsRecyclerView.setVisibility(View.GONE);
        loadAppointments(dog.ownerId);
    }

    private void loadAppointments(String ownerId) {
        db.collection("Appointments")
                .whereEqualTo("ownerId", ownerId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    appointmentsContainer.removeAllViews();
                    boolean isFirst = true;
                    boolean hasAppointments = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> appointmentMap = doc.getData();
                        if (appointmentMap != null) {
                            hasAppointments = true;
                            String date = appointmentMap.get("date") != null ? appointmentMap.get("date").toString() : "";
                            String time = appointmentMap.get("startTime") != null ? appointmentMap.get("startTime").toString() : "";
                            String type = appointmentMap.get("type") != null ? appointmentMap.get("type").toString() : "";
                            String description = appointmentMap.get("notes") != null ? appointmentMap.get("notes").toString() : "";

                            // Set last visit text for the first (most recent) appointment
                            if (isFirst) {
                                lastVisitText.setText("Last Visit: " + date + " " + time);
                                isFirst = false;
                            }

                            Button appointmentButton = new Button(getContext());
                            appointmentButton.setText(date + " " + time);
                            appointmentButton.setPadding(0, 16, 0, 16);
                            appointmentButton.setOnClickListener(v -> showAppointmentDetails(date, time, type, description));
                            appointmentsContainer.addView(appointmentButton);
                        }
                    }
                    if (!hasAppointments) {
                        TextView noAppointments = new TextView(getContext());
                        noAppointments.setText("No appointments found");
                        noAppointments.setPadding(0, 16, 0, 16);
                        appointmentsContainer.addView(noAppointments);
                        lastVisitText.setText("");
                    }
                });
    }

    private void showAppointmentDetails(String date, String time, String type, String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Appointment Details");
        builder.setMessage("Date: " + date + "\nTime: " + time + "\nType: " + type + "\nDescription: " + description);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDogList() {
        showList = true;
        detailsContainer.setVisibility(View.GONE);
        dogsRecyclerView.setVisibility(View.VISIBLE);
    }
}
