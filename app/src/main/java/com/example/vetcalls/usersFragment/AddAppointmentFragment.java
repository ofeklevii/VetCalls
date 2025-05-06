package com.example.vetcalls.usersFragment;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.vetcalls.R;
import com.example.vetcalls.notification.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddAppointmentFragment extends Fragment {
    private EditText editTitle, editTime;
    private Spinner alertSpinner1, alertSpinner2;
    private Button btnSave;
    private String selectedDate;
    private String dogId;
    private NotificationHelper notification;

    private String[] alertOptions = {"None", "During Event", "5 minutes before", "10 minutes before",
            "30 minutes before", "1 hour before", "2 hours before",
            "1 day before", "2 days before", "1 week before"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_appointment, container, false);

        notification = new NotificationHelper(requireContext());

        editTitle = view.findViewById(R.id.editTitle);
        editTime = view.findViewById(R.id.editTime);
        alertSpinner1 = view.findViewById(R.id.alertSpinner1);
        alertSpinner2 = view.findViewById(R.id.alertSpinner2);
        btnSave = view.findViewById(R.id.btnSave);

        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate");
            dogId = getArguments().getString("dogId"); // 👈 Make sure dogId is passed when this fragment is opened
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, alertOptions);
        alertSpinner1.setAdapter(adapter);
        alertSpinner2.setAdapter(adapter);

        editTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveReminder());

        requestNotificationPermission();
        return view;
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            editTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void saveReminder() {
        String title = editTitle.getText().toString().trim();
        String time = editTime.getText().toString().trim();

        if (title.isEmpty() || time.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter title and time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!time.contains(":")) {
            Toast.makeText(requireContext(), "Invalid time format", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        if (hour < 0 || minute < 0 || hour > 23 || minute > 59) {
            Toast.makeText(requireContext(), "Invalid time input", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        long reminderTimeMillis = calendar.getTimeInMillis();

        if (reminderTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Selected time is in the past. Choose a future time.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (notification == null) {
            notification = new NotificationHelper(requireContext());
        }

        notification.scheduleNotification(requireContext(), title, "Reminder for " + selectedDate, reminderTimeMillis);
        Toast.makeText(requireContext(), "Reminder Saved!", Toast.LENGTH_SHORT).show();

        // 🔥 Save to Firestore (based on whether it's vet or patient)
        saveToFirestore(title, selectedDate, time);

        returnToCalendar();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void returnToCalendar() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void saveToFirestore(String title, String date, String time) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;

        boolean isVet = userEmail != null && userEmail.contains("vet"); // You can change this logic

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("date", date);
        data.put("time", time);
        data.put("details", title);
        data.put("dogId", dogId);
        data.put("timestamp", FieldValue.serverTimestamp());

        if (isVet) {
            data.put("type", "Appointment");
            data.put("vetEmail", userEmail);

            // Save under vet's calendar
            db.collection("Vets")
                    .document(userEmail)
                    .collection("Appointments")
                    .add(data);

            // Save under global "Appointments"
            db.collection("Appointments")
                    .add(data);

            // Save under the dog's profile
            db.collection("DogProfiles")
                    .document(dogId)
                    .collection("Appointments")
                    .add(data);
        } else {
            data.put("type", "Reminder");

            // Save only under the dog's profile (private to patient)
            db.collection("DogProfiles")
                    .document(dogId)
                    .collection("Reminders")
                    .add(data);
        }
    }
}
