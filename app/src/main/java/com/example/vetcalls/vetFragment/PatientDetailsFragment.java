package com.example.vetcalls.vetFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vetcalls.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PatientDetailsFragment extends Fragment {

    private TextView nameText, birthdayText, raceText, weightText, vaccinesText, allergiesText;
    private LinearLayout appointmentsContainer;
    private FirebaseFirestore db;

    private String patientEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_details, container, false);

        nameText = view.findViewById(R.id.patientName);
        birthdayText = view.findViewById(R.id.patientBirthday);
        raceText = view.findViewById(R.id.patientRace);
        weightText = view.findViewById(R.id.patientWeight);
        vaccinesText = view.findViewById(R.id.patientVaccines);
        allergiesText = view.findViewById(R.id.patientAllergies);
        appointmentsContainer = view.findViewById(R.id.appointmentsContainer);

        db = FirebaseFirestore.getInstance();

        // קבלת הפרטים מה-Bundle
        if (getArguments() != null) {
            nameText.setText("Name: " + getArguments().getString("name"));
            birthdayText.setText("Birthday: " + getArguments().getString("birthday"));
            raceText.setText("Race: " + getArguments().getString("race"));
            weightText.setText("Weight: " + getArguments().getString("weight"));
            vaccinesText.setText("Vaccines: " + getArguments().getString("vaccines"));
            allergiesText.setText("Allergies: " + getArguments().getString("allergies"));
            patientEmail = getArguments().getString("email");

            // טעינת היסטוריית תורים
            loadAppointments(patientEmail);
        }

        return view;
    }

    private void loadAppointments(String email) {
        db.collection("Appointments")
                .whereEqualTo("patientEmail", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    appointmentsContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String type = doc.getString("type");
                        String description = doc.getString("description");

                        TextView appointmentView = new TextView(getContext());
                        appointmentView.setText("• " + date + " " + time + " - " + type + "\n" + description);
                        appointmentView.setPadding(0, 16, 0, 16);
                        appointmentsContainer.addView(appointmentView);
                    }
                });
    }
}
