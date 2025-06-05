package com.example.vetcalls.usersFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment for displaying detailed appointment information.
 * Provides comprehensive appointment details view with editing, deletion, and completion capabilities.
 * Supports both veterinarian and dog owner perspectives with appropriate action visibility.
 *
 * @author Ofek Levi
 */
public class AppointmentDetailsFragment extends Fragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";
    private static final String ARG_DETAILS = "details";
    private static final String ARG_VETERINARIAN = "veterinarian";
    private static final String ARG_TYPE = "type";
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private static final String ARG_DOG_ID = "dogId";
    private static final String ARG_VET_ID = "vetId";
    private static final String ARG_DOG_NAME = "dogName";

    private String appointmentId, dogId, vetId, date, time, type, vetName, dogName, details;
    private boolean showActions = true;
    private Button editButton, deleteButton, markCompletedButton;

    /**
     * Creates a new instance with basic appointment information for backward compatibility.
     *
     * @param date Appointment date
     * @param time Appointment time
     * @param details Appointment details/notes
     * @param veterinarian Veterinarian name
     * @param type Appointment type
     * @return New AppointmentDetailsFragment instance
     */
    public static AppointmentDetailsFragment newInstance(String date, String time, String details,
                                                         String veterinarian, String type) {
        AppointmentDetailsFragment fragment = new AppointmentDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        args.putString(ARG_TIME, time);
        args.putString(ARG_DETAILS, details);
        args.putString(ARG_VETERINARIAN, veterinarian);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new instance with full appointment information for editing and deletion capabilities.
     *
     * @param date Appointment date
     * @param time Appointment time
     * @param details Appointment details/notes
     * @param veterinarian Veterinarian name
     * @param type Appointment type
     * @param appointmentId Unique appointment identifier
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     * @param dogName Dog's name
     * @return New AppointmentDetailsFragment instance with full functionality
     */
    public static AppointmentDetailsFragment newInstanceFull(String date, String time, String details,
                                                             String veterinarian, String type, String appointmentId,
                                                             String dogId, String vetId, String dogName) {
        AppointmentDetailsFragment fragment = new AppointmentDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        args.putString(ARG_TIME, time);
        args.putString(ARG_DETAILS, details);
        args.putString(ARG_VETERINARIAN, veterinarian);
        args.putString(ARG_TYPE, type);
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        args.putString(ARG_DOG_ID, dogId);
        args.putString(ARG_VET_ID, vetId);
        args.putString(ARG_DOG_NAME, dogName);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Helper method for backward compatibility with simplified parameters.
     *
     * @param date Appointment date
     * @param time Appointment time
     * @param details Appointment details/notes
     * @param veterinarian Veterinarian name
     * @return New AppointmentDetailsFragment instance
     */
    public static AppointmentDetailsFragment newInstance(String date, String time, String details,
                                                         String veterinarian) {
        return newInstance(date, time, details, veterinarian, "");
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * Initializes UI components and loads appointment data from Firestore.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_details, container, false);

        TextView textDate = view.findViewById(R.id.textDate);
        TextView textTime = view.findViewById(R.id.textTime);
        TextView textDetails = view.findViewById(R.id.textDetails);
        TextView textVetName = view.findViewById(R.id.textVetName);
        TextView textAppointmentType = view.findViewById(R.id.textAppointmentType);
        TextView textDogName = view.findViewById(R.id.textDogName);
        ImageView backButton = view.findViewById(R.id.backButton);
        editButton = view.findViewById(R.id.editAppointmentButton);
        deleteButton = view.findViewById(R.id.deleteAppointmentButton);
        markCompletedButton = view.findViewById(R.id.markCompletedButton);
        View loadingProgressBar = view.findViewById(R.id.loadingProgressBar);

        if (getArguments() != null) {
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID, "");
            dogId = getArguments().getString(ARG_DOG_ID, "");
            vetId = getArguments().getString(ARG_VET_ID, "");
            showActions = getArguments().getBoolean("showActions", true);
        }
        android.util.Log.d("AppointmentDetails", "onCreateView showActions=" + showActions);

        setUiVisibility(view, false);
        loadingProgressBar.setVisibility(View.VISIBLE);

        if (editButton != null) editButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (deleteButton != null) deleteButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (markCompletedButton != null) markCompletedButton.setVisibility(showActions ? View.VISIBLE : View.GONE);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE);
        boolean isVet = prefs.getBoolean("isVet", false);

        if (appointmentId != null && !appointmentId.isEmpty() && dogId != null && !dogId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("DogProfiles").document(dogId).collection("Appointments").document(appointmentId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            updateUIWithAppointment(documentSnapshot);
                            setUiVisibility(view, true);
                            loadingProgressBar.setVisibility(View.GONE);

                            if (editButton != null) editButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
                            if (deleteButton != null) deleteButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
                            if (markCompletedButton != null) markCompletedButton.setVisibility(showActions && isVet ? View.VISIBLE : View.GONE);
                        } else {
                            loadingProgressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Appointment not found", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }).addOnFailureListener(e -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to load appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Missing appointment information", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        }

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    /**
     * Called immediately after onCreateView has returned.
     * Configures button visibility based on user type and action permissions.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.util.Log.d("AppointmentDetails", "onViewCreated showActions=" + showActions);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE);
        boolean isVet = prefs.getBoolean("isVet", false);
        if (editButton != null) editButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (deleteButton != null) deleteButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (markCompletedButton != null) markCompletedButton.setVisibility(showActions && isVet ? View.VISIBLE : View.GONE);
    }

    /**
     * Controls the visibility of UI components during loading states.
     *
     * @param view The root view containing UI components
     * @param visible Whether UI components should be visible
     */
    private void setUiVisibility(View view, boolean visible) {
        if (view == null) return;

        int uiVisibility = visible ? View.VISIBLE : View.GONE;
        int[] ids = new int[] {
                R.id.textDate, R.id.textTime, R.id.textDetails, R.id.textVetName, R.id.textAppointmentType,
                R.id.textDogName, R.id.appointmentTitle, R.id.backButton
        };
        for (int id : ids) {
            View v = view.findViewById(id);
            if (v != null) v.setVisibility(uiVisibility);
        }
    }

    /**
     * Navigates to the appointment editing screen with pre-filled data.
     *
     * @param appointmentId Unique appointment identifier
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     * @param date Appointment date
     */
    private void editAppointment(String appointmentId, String dogId, String vetId, String date) {
        if (appointmentId == null || dogId == null || vetId == null) {
            Toast.makeText(requireContext(), "Missing appointment information", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("selectedDate", date);
        args.putBoolean("isEdit", true);
        args.putString("appointmentId", appointmentId);
        args.putString("selectedDogId", dogId);
        args.putString("selectedVetId", vetId);

        AddAppointmentFragment editFragment = new AddAppointmentFragment();
        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Shows a confirmation dialog before deleting an appointment.
     *
     * @param appointmentId Unique appointment identifier
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     */
    private void showDeleteConfirmation(String appointmentId, String dogId, String vetId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAppointment(appointmentId, dogId, vetId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes an appointment from all relevant Firestore collections.
     *
     * @param appointmentId Unique appointment identifier
     * @param dogId Dog's unique identifier
     * @param vetId Veterinarian's unique identifier
     */
    private void deleteAppointment(String appointmentId, String dogId, String vetId) {
        if (appointmentId == null || dogId == null || vetId == null) {
            Toast.makeText(requireContext(), "Missing appointment information for deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Deleting appointment...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        FirestoreUserHelper.deleteAppointmentCompletely(appointmentId, dogId, vetId,
                () -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(requireContext(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show();

                    requireActivity().getSupportFragmentManager().popBackStack();
                },
                (error) -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(requireContext(), "Error deleting appointment: " + error, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Updates all UI fields with appointment data from Firestore document.
     *
     * @param doc Firestore document snapshot containing appointment data
     */
    private void updateUIWithAppointment(com.google.firebase.firestore.DocumentSnapshot doc) {
        date = doc.getString("date");
        time = doc.getString("startTime");
        type = doc.getString("type");
        vetName = doc.getString("vetName");
        dogName = doc.getString("dogName");
        details = doc.getString("notes");
        appointmentId = doc.getString("id");
        dogId = doc.getString("dogId");
        vetId = doc.getString("vetId");

        View view = getView();
        if (view == null) return;
        ((TextView) view.findViewById(R.id.textDate)).setText("Date: " + (date != null ? date : ""));
        ((TextView) view.findViewById(R.id.textTime)).setText("Time: " + (time != null ? time : ""));
        ((TextView) view.findViewById(R.id.textAppointmentType)).setText("Appointment type: " + (type != null ? type : ""));
        ((TextView) view.findViewById(R.id.textVetName)).setText("Veterinarian: " + (vetName != null ? vetName : ""));
        ((TextView) view.findViewById(R.id.textDogName)).setText("Dog: " + (dogName != null ? dogName : ""));
        ((TextView) view.findViewById(R.id.textDetails)).setText("Notes: " + (details == null || details.isEmpty() ? "No additional notes" : details));

        android.util.Log.d("AppointmentDetails", "updateUIWithAppointment showActions=" + showActions);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE);
        boolean isVet = prefs.getBoolean("isVet", false);

        if (editButton != null) editButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (deleteButton != null) deleteButton.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (markCompletedButton != null) markCompletedButton.setVisibility(showActions && isVet ? View.VISIBLE : View.GONE);
    }

    /**
     * Marks an appointment as completed across all relevant Firestore collections.
     * Only available to veterinarians.
     *
     * @param appointmentId Unique appointment identifier
     */
    private void markAppointmentCompleted(String appointmentId) {
        String dogId = null;
        String vetId = null;
        if (getArguments() != null) {
            dogId = getArguments().getString(ARG_DOG_ID, "");
            vetId = getArguments().getString(ARG_VET_ID, "");
        }
        if (appointmentId == null || appointmentId.isEmpty() || dogId == null || dogId.isEmpty() || vetId == null || vetId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing appointment, dog ID or vet ID", Toast.LENGTH_SHORT).show();
            return;
        }
        com.example.vetcalls.obj.FirestoreUserHelper.markAppointmentCompletedEverywhere(
                requireContext(),
                appointmentId,
                dogId,
                vetId,
                () -> Toast.makeText(requireContext(), "Appointment marked as completed", Toast.LENGTH_SHORT).show(),
                (error) -> Toast.makeText(requireContext(), "Failed to update appointment: " + error, Toast.LENGTH_SHORT).show()
        );
    }
}