
package com.example.vetcalls.usersFragment;

import static com.example.vetcalls.obj.FirestoreUserHelper.deleteAppointment;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.DogItem;
import com.example.vetcalls.obj.NotificationHelper;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.example.vetcalls.obj.VetItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment for adding or editing veterinary appointments.
 * Provides comprehensive appointment management including validation, scheduling conflicts detection,
 * reminder setup, and proper data synchronization across collections.
 *
 * @author Ofek Levi
 */
public class AddAppointmentFragment extends Fragment {

    private static final String TAG = "AddAppointmentFragment";
    private FirebaseFirestore db;

    private TextView dateTextView;
    private Spinner appointmentTypeSpinner, dogSpinner, vetSpinner, reminder1Spinner, reminder2Spinner;
    private EditText notesEditText;
    private Button timeButton, saveButton;

    private String selectedDate, selectedDogId, selectedVetId, appointmentId, userId;
    private String selectedTime = "", endTime, appointmentType;
    private boolean isVet, isEdit = false;
    private long appointmentDurationMinutes = 20;
    private NotificationHelper notificationHelper;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_appointment, container, false);
    }

    /**
     * Called immediately after onCreateView has returned.
     * Initializes all UI components, data, and event listeners.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initData();
        setupArguments();
        setupSpinners();
        setupListeners();

        if (isEdit && !appointmentId.isEmpty()) {
            loadAppointmentDataFromArguments();
        }
    }

    /**
     * Initializes all UI components and configures their initial visibility.
     *
     * @param view The root view of the fragment
     */
    private void initViews(View view) {
        dateTextView = view.findViewById(R.id.dateTextView);
        appointmentTypeSpinner = view.findViewById(R.id.appointmentTypeSpinner);
        dogSpinner = view.findViewById(R.id.dogSpinner);
        vetSpinner = view.findViewById(R.id.vetSpinner);
        notesEditText = view.findViewById(R.id.notesEditText);
        timeButton = view.findViewById(R.id.timeButton);
        reminder1Spinner = view.findViewById(R.id.reminder1Spinner);
        reminder2Spinner = view.findViewById(R.id.reminder2Spinner);
        saveButton = view.findViewById(R.id.saveButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);

        timeButton.setVisibility(View.GONE);
        view.findViewById(R.id.timeLabel).setVisibility(View.GONE);
        view.findViewById(R.id.userMessageTextView).setVisibility(View.GONE);

        if (isEdit && !appointmentId.isEmpty()) {
            deleteButton.setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.titleTextView)).setText("Editing an appointment");
        }
    }

    /**
     * Initializes data sources and determines user type.
     */
    private void initData() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationHelper = new NotificationHelper(requireContext());

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        isVet = prefs.getBoolean("isVet", false);
    }

    /**
     * Sets up fragment arguments and displays the selected date.
     */
    private void setupArguments() {
        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate", "");
            isEdit = getArguments().getBoolean("isEdit", false);
            appointmentId = getArguments().getString("appointmentId", "");
            dateTextView.setText("Date: " + selectedDate);
        }
    }

    /**
     * Initializes all spinners with their respective data.
     */
    private void setupSpinners() {
        setupAppointmentTypeSpinner();
        setupReminderSpinners();
        loadDogs();
        loadVets();
    }

    /**
     * Sets up event listeners for all interactive UI components.
     */
    private void setupListeners() {
        appointmentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                appointmentType = parent.getItemAtPosition(position).toString();
                updateAppointmentDuration(appointmentType);
                updateTimeSpinnerVisibility();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        dogSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DogItem selectedDog = (DogItem) parent.getItemAtPosition(position);
                selectedDogId = selectedDog.getId();
                updateTimeSpinnerVisibility();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        vetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VetItem selectedVet = (VetItem) parent.getItemAtPosition(position);
                selectedVetId = selectedVet.getId();
                updateTimeSpinnerVisibility();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        timeButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new android.app.TimePickerDialog(getContext(),
                    (view1, hourOfDay, minute1) -> {
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                        timeButton.setText(selectedTime);
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        saveButton.setOnClickListener(v -> saveAppointment());

        requireView().findViewById(R.id.deleteButton).setOnClickListener(v -> {
            if (isEdit && appointmentId != null && !appointmentId.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete appointment")
                        .setMessage("Are you sure you want to delete this appointment?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirestoreUserHelper.deleteAppointmentCompletely(
                                    appointmentId,
                                    selectedDogId,
                                    selectedVetId,
                                    () -> {
                                        Toast.makeText(requireContext(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show();
                                        requireActivity().getSupportFragmentManager().popBackStack();
                                    },
                                    (errorMessage) -> {
                                        Log.e(TAG, "Error deleting appointment: " + errorMessage);
                                        Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                                    }
                            );
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    /**
     * Loads appointment data from arguments when in edit mode.
     */
    private void loadAppointmentDataFromArguments() {
        if (getArguments() != null) {
            selectedDogId = getArguments().getString("selectedDogId", "");
            selectedVetId = getArguments().getString("selectedVetId", "");

            if (!selectedDogId.isEmpty() && !appointmentId.isEmpty()) {
                loadAppointmentDataFromFirestore();
            }
        }
    }

    /**
     * Loads existing appointment data from Firestore for editing.
     */
    private void loadAppointmentDataFromFirestore() {
        db.collection("DogProfiles")
                .document(selectedDogId)
                .collection("Appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            setSpinnerSelection(appointmentTypeSpinner, (String) data.get("type"));
                            notesEditText.setText((String) data.get("notes"));

                            String startTime = (String) data.get("startTime");
                            if (startTime != null) {
                                selectedTime = startTime;
                                timeButton.setText(selectedTime);
                            }

                            updateDogSpinnerSelection(selectedDogId);
                            updateVetSpinnerSelection(selectedVetId);
                            updateTimeSpinnerVisibility();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointment", e);
                    Toast.makeText(requireContext(), "Error loading appointment data", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the dog spinner selection to match the provided dog ID.
     *
     * @param dogId The dog ID to select in the spinner
     */
    private void updateDogSpinnerSelection(String dogId) {
        ArrayAdapter<DogItem> adapter = (ArrayAdapter<DogItem>) dogSpinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                DogItem item = adapter.getItem(i);
                if (item != null && item.getId().equals(dogId)) {
                    dogSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Updates the veterinarian spinner selection to match the provided vet ID.
     *
     * @param vetId The veterinarian ID to select in the spinner
     */
    private void updateVetSpinnerSelection(String vetId) {
        if (!isVet) {
            ArrayAdapter<VetItem> adapter = (ArrayAdapter<VetItem>) vetSpinner.getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    VetItem item = adapter.getItem(i);
                    if (item != null && item.getId().equals(vetId)) {
                        vetSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Initiates the appointment saving process with validation and conflict checking.
     */
    private void saveAppointment() {
        if (!validateInputs()) return;

        if (isEdit && !appointmentId.isEmpty()) {
            checkIfTimeChangedAndValidate();
        } else {
            proceedWithSave();
        }
    }

    /**
     * Validates all required input fields before saving.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs() {
        if (selectedDogId == null || selectedDogId.isEmpty()) {
            Toast.makeText(requireContext(), "You have to choose a dog", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedVetId == null || selectedVetId.isEmpty()) {
            Toast.makeText(requireContext(), "You have to choose a veterinarian", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(requireContext(), "You have to choose a time", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (appointmentType == null || appointmentType.isEmpty()) {
            Toast.makeText(requireContext(), "You must select an appointment type", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Checks if the appointment time has changed during editing and validates accordingly.
     */
    private void checkIfTimeChangedAndValidate() {
        db.collection("DogProfiles")
                .document(selectedDogId)
                .collection("Appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> originalData = documentSnapshot.getData();
                        if (originalData != null) {
                            String originalDate = (String) originalData.get("date");
                            String originalTime = (String) originalData.get("startTime");

                            boolean dateChanged = !selectedDate.equals(originalDate);
                            boolean timeChanged = !selectedTime.equals(originalTime);

                            if (dateChanged || timeChanged) {
                                validateNewTimeAndSave();
                            } else {
                                proceedWithSave();
                            }
                        }
                    } else {
                        proceedWithSave();
                    }
                })
                .addOnFailureListener(e -> proceedWithSave());
    }

    /**
     * Validates the new appointment time against existing appointments to prevent conflicts.
     */
    private void validateNewTimeAndSave() {
        db.collection("Veterinarians")
                .document(selectedVetId)
                .collection("Appointments")
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    boolean timeAvailable = true;
                    String conflictMessage = "";

                    for (QueryDocumentSnapshot document : querySnapshots) {
                        if (document.getId().equals(appointmentId)) continue;

                        Map<String, Object> data = document.getData();
                        String existingStart = (String) data.get("startTime");
                        String existingEnd = (String) data.get("endTime");

                        if (existingStart != null && existingEnd != null) {
                            int existingStartMin = convertTimeToMinutes(existingStart);
                            int existingEndMin = convertTimeToMinutes(existingEnd);
                            int newStartMin = convertTimeToMinutes(selectedTime);
                            int newEndMin = newStartMin + (int) appointmentDurationMinutes;

                            if ((newStartMin >= existingStartMin && newStartMin < existingEndMin) ||
                                    (newEndMin > existingStartMin && newEndMin <= existingEndMin) ||
                                    (newStartMin <= existingStartMin && newEndMin >= existingEndMin)) {
                                timeAvailable = false;
                                conflictMessage = "Time conflicts with existing appointment: " + existingStart + " - " + existingEnd;
                                break;
                            }
                        }
                    }

                    if (timeAvailable) {
                        proceedWithSave();
                    } else {
                        Toast.makeText(requireContext(), conflictMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Proceeds with saving the appointment after all validations pass.
     */
    private void proceedWithSave() {
        calculateEndTime();

        if (!isEdit || appointmentId.isEmpty()) {
            appointmentId = UUID.randomUUID().toString();
        }

        Map<String, Object> appointmentData = createAppointmentData();
        addReminders(appointmentData);

        FirestoreUserHelper.addAppointment(appointmentId, appointmentData);

        Toast.makeText(requireContext(), isEdit ? "Appointment updated successfully" : "Appointment created successfully", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    /**
     * Creates the appointment data map for Firestore storage.
     *
     * @return Map containing all appointment data
     */
    private Map<String, Object> createAppointmentData() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", appointmentId);
        data.put("date", selectedDate);
        data.put("startTime", selectedTime);
        data.put("endTime", endTime);
        data.put("type", appointmentType);
        data.put("dogId", selectedDogId);
        data.put("vetId", selectedVetId);
        data.put("ownerId", userId);
        data.put("notes", notesEditText.getText().toString());
        data.put("completed", false);

        if (dogSpinner.getSelectedItem() instanceof DogItem) {
            data.put("dogName", ((DogItem) dogSpinner.getSelectedItem()).getName());
        }

        if (isVet) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("VetProfile", Context.MODE_PRIVATE);
            String fullName = prefs.getString("fullName", "Veterinarian");
            data.put("vetName", fullName);
        } else if (vetSpinner.getSelectedItem() instanceof VetItem) {
            data.put("vetName", ((VetItem) vetSpinner.getSelectedItem()).getName());
        }

        return data;
    }

    /**
     * Adds reminder notifications for the appointment.
     *
     * @param appointmentData The appointment data map
     */
    private void addReminders(Map<String, Object> appointmentData) {
        if (isVet) return;

        String reminder1 = reminder1Spinner.getSelectedItem().toString();
        String reminder2 = reminder2Spinner.getSelectedItem().toString();

        if (!reminder1.equals("No reminder") || !reminder2.equals("No reminder")) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault());
                Date appointmentDateTime = format.parse(selectedDate + " " + selectedTime);
                long appointmentTime = appointmentDateTime.getTime();

                if (!reminder1.equals("No reminder")) {
                    createReminder(reminder1, appointmentTime);
                }
                if (!reminder2.equals("No reminder")) {
                    createReminder(reminder2, appointmentTime);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date/time", e);
            }
        }
    }

    /**
     * Creates a specific reminder notification and stores it in Firestore.
     *
     * @param reminderOption The reminder timing option
     * @param appointmentTime The appointment time in milliseconds
     */
    private void createReminder(String reminderOption, long appointmentTime) {
        long reminderTime = getReminderTime(reminderOption, appointmentTime);

        notificationHelper.scheduleNotification(
                requireContext(),
                "Reminder: " + appointmentType,
                "You have an appointment on " + selectedDate + " at " + selectedTime,
                reminderTime
        );

        Map<String, Object> reminderData = new HashMap<>();
        reminderData.put("id", UUID.randomUUID().toString());
        reminderData.put("title", "Reminder: " + appointmentType);
        reminderData.put("description", "You have an appointment on " + selectedDate + " at " + selectedTime);
        reminderData.put("time", new Timestamp(new Date(reminderTime)));
        reminderData.put("appointmentId", appointmentId);

        FirestoreUserHelper.addReminderToUser(userId, (String) reminderData.get("id"), reminderData);
    }

    /**
     * Calculates the end time based on selected time and appointment duration.
     */
    private void calculateEndTime() {
        if (selectedTime != null && !selectedTime.isEmpty()) {
            int startMinutes = convertTimeToMinutes(selectedTime);
            int endMinutes = startMinutes + (int) appointmentDurationMinutes;
            endTime = convertMinutesToTime(endMinutes);
        }
    }

    /**
     * Converts time string to minutes for calculation purposes.
     *
     * @param time Time in HH:MM format
     * @return Time in minutes from midnight
     */
    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * Converts minutes to time string format.
     *
     * @param minutes Minutes from midnight
     * @return Time in HH:MM format
     */
    private String convertMinutesToTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
    }

    /**
     * Updates appointment duration based on appointment type.
     *
     * @param type The appointment type
     */
    private void updateAppointmentDuration(String type) {
        switch (type) {
            case "Vaccination and blood tests": appointmentDurationMinutes = 20; break;
            case "Routine tests": appointmentDurationMinutes = 60; break;
            case "Surgery": appointmentDurationMinutes = 180; break;
            case "Urgent treatment": appointmentDurationMinutes = 60; break;
            default: appointmentDurationMinutes = 20;
        }
    }

    /**
     * Updates the visibility of time selection components based on selections.
     */
    private void updateTimeSpinnerVisibility() {
        TextView timeLabel = requireView().findViewById(R.id.timeLabel);
        TextView messageView = requireView().findViewById(R.id.userMessageTextView);

        boolean dogSelected = selectedDogId != null && !selectedDogId.isEmpty();
        boolean vetSelected = selectedVetId != null && !selectedVetId.isEmpty();

        if (dogSelected && vetSelected) {
            timeButton.setVisibility(View.VISIBLE);
            timeLabel.setVisibility(View.VISIBLE);
            messageView.setVisibility(View.GONE);
        } else {
            timeButton.setVisibility(View.GONE);
            timeLabel.setVisibility(View.GONE);
            messageView.setVisibility(View.VISIBLE);
            messageView.setText("Please select both a dog and a veterinarian");
        }
    }

    /**
     * Sets up the appointment type spinner with predefined options.
     */
    private void setupAppointmentTypeSpinner() {
        String[] types = {"Vaccination and blood tests", "Routine tests", "Surgery", "Urgent treatment"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentTypeSpinner.setAdapter(adapter);
    }

    /**
     * Sets up the reminder spinners with timing options.
     */
    private void setupReminderSpinners() {
        String[] options = {"No reminder", "At appointment time", "5 minutes before", "10 minutes before",
                "15 minutes before", "30 minutes before", "1 hour before", "2 hours before",
                "1 day before", "2 days before", "1 week before"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reminder1Spinner.setAdapter(adapter);
        reminder2Spinner.setAdapter(adapter);
    }

    /**
     * Loads dogs from Firestore based on user type.
     */
    private void loadDogs() {
        ArrayList<DogItem> dogs = new ArrayList<>();
        dogs.add(new DogItem("", "Choose a dog..."));

        if (isVet) {
            db.collection("DogProfiles")
                    .whereEqualTo("vetId", userId)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Map<String, Object> data = doc.getData();
                            String dogId = doc.getId();
                            String dogName = (String) data.get("name");
                            String ownerId = (String) data.get("ownerId");
                            dogs.add(new DogItem(dogId, dogName + " (Owner: " + ownerId + ")"));
                        }
                        setDogAdapter(dogs);
                    });
        } else {
            db.collection("Users")
                    .document(userId)
                    .collection("Dogs")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Map<String, Object> data = doc.getData();
                            String dogId = (String) data.get("dogId");
                            String dogName = (String) data.get("name");
                            if (dogId != null && dogName != null) {
                                dogs.add(new DogItem(dogId, dogName));
                            }
                        }
                        setDogAdapter(dogs);

                        if (dogs.size() == 2) {
                            dogSpinner.setSelection(1);
                            selectedDogId = dogs.get(1).getId();
                        }
                    });
        }
    }

    /**
     * Sets the adapter for the dog spinner.
     *
     * @param dogs List of DogItem objects to display
     */
    private void setDogAdapter(ArrayList<DogItem> dogs) {
        ArrayAdapter<DogItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, dogs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dogSpinner.setAdapter(adapter);
    }

    /**
     * Loads veterinarians from Firestore or hides the spinner for vet users.
     */
    private void loadVets() {
        if (isVet) {
            vetSpinner.setVisibility(View.GONE);
            requireView().findViewById(R.id.vetLabel).setVisibility(View.GONE);
            selectedVetId = userId;
        } else {
            ArrayList<VetItem> vets = new ArrayList<>();
            vets.add(new VetItem("", "Choose a vet..."));

            db.collection("Veterinarians")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Map<String, Object> data = doc.getData();
                            String vetId = doc.getId();
                            String name = (String) data.get("fullName");
                            vets.add(new VetItem(vetId, name != null ? name : "Veterinarian"));
                        }

                        ArrayAdapter<VetItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vets);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        vetSpinner.setAdapter(adapter);
                    });
        }
    }

    /**
     * Sets the selection of a spinner to match a specific value.
     *
     * @param spinner The spinner to update
     * @param value The value to select
     */
    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    /**
     * Calculates the reminder time based on the selected option.
     *
     * @param option The reminder timing option
     * @param appointmentTime The appointment time in milliseconds
     * @return The reminder time in milliseconds
     */
    private long getReminderTime(String option, long appointmentTime) {
        switch (option) {
            case "At appointment time": return appointmentTime;
            case "5 minutes before": return appointmentTime - (5 * 60 * 1000);
            case "10 minutes before": return appointmentTime - (10 * 60 * 1000);
            case "15 minutes before": return appointmentTime - (15 * 60 * 1000);
            case "30 minutes before": return appointmentTime - (30 * 60 * 1000);
            case "1 hour before": return appointmentTime - (60 * 60 * 1000);
            case "2 hours before": return appointmentTime - (2 * 60 * 60 * 1000);
            case "1 day before": return appointmentTime - (24 * 60 * 60 * 1000);
            case "2 days before": return appointmentTime - (2 * 24 * 60 * 60 * 1000);
            case "1 week before": return appointmentTime - (7 * 24 * 60 * 60 * 1000);
            default: return 0;
        }
    }
}