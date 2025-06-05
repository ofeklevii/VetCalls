package com.example.vetcalls.usersFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import java.text.SimpleDateFormat;
import android.util.Log;

/**
 * Fragment that displays the history of completed appointments for the current user's dogs.
 * Shows a RecyclerView with appointment details or an empty state message when no appointments exist.
 *
 * @author Ofek Levi
 */
public class HistoryFragment extends Fragment {

    /** RecyclerView for displaying the list of completed appointments */
    private RecyclerView recyclerView;

    /** Adapter for managing appointment data in the RecyclerView */
    private AppointmentAdapter adapter;

    /** List containing all completed appointment data */
    private List<Map<String, Object>> appointmentList;

    /** TextView displayed when no appointment history exists */
    private TextView emptyHistoryText;

    /** Firebase Firestore database instance */
    private FirebaseFirestore db;

    /** Current authenticated user's unique identifier */
    private String userId;

    /**
     * Creates and initializes the fragment view with all UI components.
     * Sets up the RecyclerView, adapter, and loads completed appointments.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment's UI will be attached to
     * @param savedInstanceState Bundle containing the fragment's previously saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        Log.d("HistoryDebug", "onCreateView called");

        initializeUIComponents(view);
        initializeFirebase();

        if (!authenticateUser()) {
            showEmptyState();
            return view;
        }

        setupRecyclerView();
        loadCompletedAppointments();

        return view;
    }

    /**
     * Called when the fragment becomes visible to the user again.
     * Refreshes the appointment list to ensure data is up to date.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadCompletedAppointments();
    }

    /**
     * Initializes UI components from the layout.
     *
     * @param view The root view of the fragment
     */
    private void initializeUIComponents(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyHistoryText = view.findViewById(R.id.emptyHistoryText);
    }

    /**
     * Initializes Firebase Firestore database instance.
     */
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Authenticates the current user and retrieves their user ID.
     *
     * @return true if user is authenticated, false otherwise
     */
    private boolean authenticateUser() {
        try {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets up the RecyclerView with layout manager and adapter.
     */
    private void setupRecyclerView() {
        appointmentList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList, requireActivity(), false);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Loads all completed appointments for the current user's dogs from Firestore.
     * First retrieves all dogs owned by the user, then fetches completed appointments for each dog.
     * Handles duplicate removal and updates the UI accordingly.
     */
    private void loadCompletedAppointments() {
        appointmentList.clear();
        Log.d("HistoryDebug", "Start loading completed appointments for userId: " + userId);

        db.collection("DogProfiles")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(dogSnapshots -> {
                    List<String> dogIds = extractDogIds(dogSnapshots);
                    Log.d("HistoryDebug", "Found " + dogIds.size() + " dogs for userId: " + userId);

                    if (dogIds.isEmpty()) {
                        handleEmptyDogList();
                        return;
                    }

                    loadAppointmentsForDogs(dogIds);
                });
    }

    /**
     * Extracts dog IDs from Firestore query snapshots.
     *
     * @param dogSnapshots The query snapshots containing dog documents
     * @return List of dog IDs
     */
    private List<String> extractDogIds(Iterable<QueryDocumentSnapshot> dogSnapshots) {
        List<String> dogIds = new ArrayList<>();
        for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
            dogIds.add(dogDoc.getId());
        }
        return dogIds;
    }

    /**
     * Handles the case when no dogs are found for the current user.
     */
    private void handleEmptyDogList() {
        adapter.updateAppointments(appointmentList);
        showEmptyState();
    }

    /**
     * Loads completed appointments for all provided dog IDs.
     * Uses a counter to track completion of all async operations.
     *
     * @param dogIds List of dog IDs to load appointments for
     */
    private void loadAppointmentsForDogs(List<String> dogIds) {
        final int[] finished = {0};
        final int total = dogIds.size();

        for (String dogId : dogIds) {
            loadAppointmentsForSingleDog(dogId, finished, total);
        }
    }

    /**
     * Loads completed appointments for a single dog.
     *
     * @param dogId The ID of the dog to load appointments for
     * @param finished Array containing the count of finished operations
     * @param total Total number of operations to complete
     */
    private void loadAppointmentsForSingleDog(String dogId, int[] finished, int total) {
        db.collection("DogProfiles")
                .document(dogId)
                .collection("Appointments")
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(appointmentsSnapshot -> {
                    Log.d("HistoryDebug", "Dog " + dogId + " has " + appointmentsSnapshot.size() + " completed appointments");

                    addAppointmentsToList(appointmentsSnapshot);
                    handleOperationComplete(finished, total);
                })
                .addOnFailureListener(e -> {
                    Log.e("HistoryDebug", "Failed to load appointments for dog " + dogId, e);
                    handleOperationComplete(finished, total);
                });
    }

    /**
     * Adds appointments from Firestore snapshot to the appointment list.
     *
     * @param appointmentsSnapshot The Firestore query snapshot containing appointments
     */
    private void addAppointmentsToList(Iterable<QueryDocumentSnapshot> appointmentsSnapshot) {
        for (QueryDocumentSnapshot appointmentDoc : appointmentsSnapshot) {
            Map<String, Object> appointmentData = appointmentDoc.getData();
            Log.d("HistoryDebug", "Appointment data: " + appointmentData);
            appointmentList.add(appointmentData);
        }
    }

    /**
     * Handles the completion of a single async operation.
     * When all operations are complete, processes the final appointment list.
     *
     * @param finished Array containing the count of finished operations
     * @param total Total number of operations to complete
     */
    private void handleOperationComplete(int[] finished, int total) {
        finished[0]++;
        if (finished[0] == total) {
            Log.d("HistoryDebug", "Total completed appointments loaded: " + appointmentList.size());
            processAndDisplayAppointments();
        }
    }

    /**
     * Processes the loaded appointments by removing duplicates and updating the UI.
     */
    private void processAndDisplayAppointments() {
        removeDuplicateAppointments();
        adapter.updateAppointments(appointmentList);

        if (appointmentList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    /**
     * Removes duplicate appointments from the list based on appointment ID.
     */
    private void removeDuplicateAppointments() {
        Map<String, Map<String, Object>> uniqueAppointments = new HashMap<>();
        for (Map<String, Object> appt : appointmentList) {
            String id = (String) appt.get("id");
            if (id != null) {
                uniqueAppointments.put(id, appt);
            }
        }
        appointmentList = new ArrayList<>(uniqueAppointments.values());
    }

    /**
     * Shows the empty state UI when no appointments are available.
     * Hides the RecyclerView and shows the empty state text.
     */
    private void showEmptyState() {
        if (emptyHistoryText != null) {
            emptyHistoryText.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    /**
     * Hides the empty state UI when appointments are available.
     * Shows the RecyclerView and hides the empty state text.
     */
    private void hideEmptyState() {
        if (emptyHistoryText != null) {
            emptyHistoryText.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}