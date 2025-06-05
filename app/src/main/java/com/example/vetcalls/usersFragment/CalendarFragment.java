package com.example.vetcalls.usersFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for displaying and managing appointments in a calendar interface.
 * Provides calendar view with date selection and appointment list display.
 * Supports different views for veterinarians and dog owners with appropriate functionality.
 *
 * @author Ofek Levi
 */
public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarFragment";

    private CalendarView calendarView;
    private RecyclerView appointmentsRecyclerView;
    private AppointmentAdapter appointmentAdapter;
    private List<Map<String, Object>> appointmentList;
    private FirebaseFirestore db;
    private boolean isVet;
    private SharedPreferences sharedPreferences;
    private String selectedDate = "";
    private String userId;
    private Button addAppointmentButton;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * Initializes UI components and loads appointments for the current date.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initViews(view);
        initData();
        setupCalendar();
        setupAddButton();

        selectedDate = getTodayDateString();
        loadAppointments(selectedDate);

        return view;
    }

    /**
     * Initializes all UI components and sets up the RecyclerView.
     *
     * @param view The root view of the fragment
     */
    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView);
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton);

        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, requireActivity(), true);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentsRecyclerView.setAdapter(appointmentAdapter);
    }

    /**
     * Initializes data sources and determines user type from SharedPreferences.
     */
    private void initData() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        isVet = sharedPreferences.getBoolean("isVet", false);

        Log.d(TAG, "User initialized - userId: " + userId + ", isVet: " + isVet);
    }

    /**
     * Sets up the calendar view with date change listener.
     */
    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%d-%d-%d", year, month + 1, dayOfMonth);
            Log.d(TAG, "Date selected: " + selectedDate);
            loadAppointments(selectedDate);
        });
    }

    /**
     * Configures the add appointment button based on user type.
     */
    private void setupAddButton() {
        if (isVet) {
            addAppointmentButton.setText("Add appointment for patient");
        } else {
            addAppointmentButton.setText("Make an appointment");
        }
        addAppointmentButton.setOnClickListener(v -> openAddAppointmentFragment());
    }

    /**
     * Opens the add appointment fragment with the selected date.
     */
    private void openAddAppointmentFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("selectedDate", selectedDate);
        bundle.putBoolean("isVet", isVet);

        AddAppointmentFragment addAppointmentFragment = new AddAppointmentFragment();
        addAppointmentFragment.setArguments(bundle);

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, addAppointmentFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Loads appointments for the specified date based on user type.
     *
     * @param date The date to load appointments for in yyyy-M-d format
     */
    private void loadAppointments(String date) {
        Log.d(TAG, "Loading appointments for date: " + date + ", isVet: " + isVet);

        appointmentList.clear();
        appointmentAdapter.notifyDataSetChanged();

        if (isVet) {
            loadVetAppointments(date);
        } else {
            loadPatientAppointments(date);
        }
    }

    /**
     * Loads appointments for veterinarian users from their appointments collection.
     *
     * @param date The date to load appointments for
     */
    private void loadVetAppointments(String date) {
        Log.d(TAG, "Loading vet appointments for: " + date);

        db.collection("Veterinarians")
                .document(userId)
                .collection("Appointments")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    Log.d(TAG, "Vet appointments query successful. Found: " + querySnapshots.size());

                    appointmentList.clear();

                    for (QueryDocumentSnapshot document : querySnapshots) {
                        Map<String, Object> appointmentData = document.getData();
                        appointmentData.put("documentId", document.getId());
                        boolean isCompleted = appointmentData.get("completed") instanceof Boolean && (Boolean) appointmentData.get("completed");
                        String apptDate = (String) appointmentData.get("date");
                        String apptTime = (String) appointmentData.get("startTime");
                        if (!isCompleted) {
                            if (!isFutureAppointment(apptDate, apptTime)) {
                                markAppointmentCompletedForVet(document.getId(), (String)appointmentData.get("dogId"), userId);
                                continue;
                            }
                            appointmentList.add(appointmentData);
                            Log.d(TAG, "Added vet appointment: " + appointmentData.get("type") + " at " + appointmentData.get("startTime"));
                        }
                    }

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading vet appointments", e);
                    Toast.makeText(getContext(), "Error loading appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    /**
     * Loads appointments for dog owner users by querying their dogs' appointments.
     *
     * @param date The date to load appointments for
     */
    private void loadPatientAppointments(String date) {
        Log.d(TAG, "Loading patient appointments for: " + date);
        Log.d(TAG, "selectedDate: " + date);
        db.collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(dogSnapshots -> {
                    List<String> dogIds = new ArrayList<>();
                    if (!dogSnapshots.isEmpty()) {
                        Log.d(TAG, "Found " + dogSnapshots.size() + " dogs for user (Users collection)");
                        for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                            String dogId = dogDoc.getString("dogId");
                            if (dogId != null) dogIds.add(dogId);
                        }
                    }
                    db.collection("DogProfiles")
                            .whereEqualTo("ownerId", userId)
                            .get()
                            .addOnSuccessListener(dogProfileSnapshots -> {
                                for (QueryDocumentSnapshot dogProfileDoc : dogProfileSnapshots) {
                                    String dogId = dogProfileDoc.getId();
                                    if (!dogIds.contains(dogId)) {
                                        dogIds.add(dogId);
                                    }
                                }
                                Log.d(TAG, "All dogIds for user: " + dogIds);
                                if (dogIds.isEmpty()) {
                                    Log.d(TAG, "No dogs found for user (DogProfiles)");
                                    updateUI();
                                    return;
                                }
                                Log.d(TAG, "Total dogs for user: " + dogIds.size());
                                List<Task<QuerySnapshot>> appointmentTasks = new ArrayList<>();
                                for (String dogId : dogIds) {
                                    Task<QuerySnapshot> appointmentTask = db.collection("DogProfiles")
                                            .document(dogId)
                                            .collection("Appointments")
                                            .whereEqualTo("date", date)
                                            .get();
                                    appointmentTasks.add(appointmentTask);
                                }
                                Tasks.whenAllComplete(appointmentTasks)
                                        .addOnCompleteListener(allTasks -> {
                                            appointmentList.clear();
                                            int taskIndex = 0;
                                            for (String dogId : dogIds) {
                                                if (taskIndex < allTasks.getResult().size()) {
                                                    Task<?> task = allTasks.getResult().get(taskIndex);
                                                    if (task.isSuccessful()) {
                                                        QuerySnapshot appointmentSnapshot = (QuerySnapshot) task.getResult();
                                                        Log.d(TAG, "Found " + appointmentSnapshot.size() + " appointments for dogId: " + dogId);
                                                        for (QueryDocumentSnapshot appointmentDoc : appointmentSnapshot) {
                                                            Map<String, Object> appointmentData = appointmentDoc.getData();
                                                            appointmentData.put("dogName", appointmentData.get("dogName"));
                                                            appointmentData.put("documentId", appointmentDoc.getId());
                                                            String apptDate = (String) appointmentData.get("date");
                                                            String apptTime = (String) appointmentData.get("startTime");
                                                            String apptOwner = (String) appointmentData.get("ownerId");
                                                            Log.d(TAG, "Appointment candidate: date=" + apptDate + ", startTime=" + apptTime + ", ownerId=" + apptOwner);
                                                            if (shouldShowAppointment(apptDate, apptTime)) {
                                                                appointmentList.add(appointmentData);
                                                                Log.d(TAG, "Added patient appointment: " + appointmentData.get("type") + " for dogId: " + dogId);
                                                            }
                                                        }
                                                    } else {
                                                        Log.e(TAG, "Error loading appointments for dogId: " + dogId, task.getException());
                                                    }
                                                }
                                                taskIndex++;
                                            }
                                            Log.d(TAG, "appointmentList size: " + appointmentList.size());
                                            for (Map<String, Object> appt : appointmentList) {
                                                Log.d(TAG, "Appointment in list: " + appt.toString());
                                            }
                                            updateUI();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading user dog profiles", e);
                                updateUI();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user dogs", e);
                    updateUI();
                });
    }

    /**
     * Determines whether an appointment should be displayed based on its date and time.
     *
     * @param date The appointment date
     * @param startTime The appointment start time
     * @return true if the appointment should be shown, false otherwise
     */
    private boolean shouldShowAppointment(String date, String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
            Date appointmentDate = sdf.parse(date);
            Calendar apptCal = Calendar.getInstance();
            apptCal.setTime(appointmentDate);
            Calendar now = Calendar.getInstance();
            if (apptCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    apptCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    apptCal.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                return true;
            }
            return appointmentDate != null && appointmentDate.after(now.getTime());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Checks if an appointment is in the future based on date and time.
     *
     * @param date The appointment date
     * @param startTime The appointment start time
     * @return true if the appointment is in the future, false otherwise
     */
    private boolean isFutureAppointment(String date, String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault());
            Date appointmentDate = sdf.parse(date + " " + startTime);
            return appointmentDate != null && appointmentDate.after(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Marks an appointment as completed in all relevant Firestore collections.
     *
     * @param appointmentId The appointment's unique identifier
     * @param dogId The dog's unique identifier
     * @param vetId The veterinarian's unique identifier
     */
    private void markAppointmentCompletedEverywhere(String appointmentId, String dogId, String vetId) {
        db.collection("Veterinarians")
                .document(vetId)
                .collection("Appointments")
                .document(appointmentId)
                .update("completed", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment marked as completed for vet: " + appointmentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update appointment for vet: " + appointmentId, e));

        db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .document(appointmentId)
                .update("completed", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment marked as completed for dog: " + appointmentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update appointment for dog: " + appointmentId, e));
    }

    /**
     * Marks an appointment as completed using the FirestoreUserHelper utility.
     *
     * @param appointmentId The appointment's unique identifier
     * @param dogId The dog's unique identifier
     * @param vetId The veterinarian's unique identifier
     */
    private void markAppointmentCompletedForVet(String appointmentId, String dogId, String vetId) {
        com.example.vetcalls.obj.FirestoreUserHelper.markAppointmentCompletedEverywhere(
                getContext(),
                appointmentId,
                dogId,
                vetId,
                null,
                (error) -> Log.e(TAG, "Failed to update appointment: " + error)
        );
    }

    /**
     * Updates the UI to reflect the current appointment list state.
     */
    private void updateUI() {
        appointmentAdapter.notifyDataSetChanged();

        if (appointmentList.isEmpty()) {
            showEmptyView("No appointments for " + selectedDate);
        } else {
            showAppointmentsView();
            Log.d(TAG, "UI updated with " + appointmentList.size() + " appointments");
        }
    }

    /**
     * Shows the empty view when no appointments are available.
     *
     * @param message The message to display in the empty view
     */
    private void showEmptyView(String message) {
        View root = getView();
        if (root == null) return;

        TextView emptyView = root.findViewById(R.id.emptyView);
        if (emptyView != null) {
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
        }

        if (appointmentsRecyclerView != null) {
            appointmentsRecyclerView.setVisibility(View.GONE);
        }

        Log.d(TAG, "Showing empty view: " + message);
    }

    /**
     * Shows the appointments list view when appointments are available.
     */
    private void showAppointmentsView() {
        View root = getView();
        if (root == null) return;

        TextView emptyView = root.findViewById(R.id.emptyView);
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        if (appointmentsRecyclerView != null) {
            appointmentsRecyclerView.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Showing appointments view");
    }

    /**
     * Gets today's date formatted as a string.
     *
     * @return Today's date in yyyy-M-d format
     */
    private String getTodayDateString() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
        String today = dateFormat.format(calendar.getTime());
        Log.d(TAG, "Today's date: " + today);
        return today;
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Refreshes appointment data for the selected date.
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        if (!selectedDate.isEmpty()) {
            Log.d(TAG, "Refreshing appointments for: " + selectedDate);
            loadAppointments(selectedDate);
        } else {
            selectedDate = getTodayDateString();
            loadAppointments(selectedDate);
        }
    }
}