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

public class AppointmentDetailsFragment extends Fragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";
    private static final String ARG_DETAILS = "details";
    private static final String ARG_VETERINARIAN = "veterinarian";
    private static final String ARG_TYPE = "type";
    // פרמטרים נוספים לעריכה ומחיקה
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private static final String ARG_DOG_ID = "dogId";
    private static final String ARG_VET_ID = "vetId";
    private static final String ARG_DOG_NAME = "dogName";

    // מתודה קיימת - תאימות לאחור
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

    // מתודה חדשה עם כל הפרטים לעריכה ומחיקה
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

    // מתודת עזר לתאימות לאחור
    public static AppointmentDetailsFragment newInstance(String date, String time, String details,
                                                         String veterinarian) {
        return newInstance(date, time, details, veterinarian, "");
    }

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
        ImageView backButton = view.findViewById(R.id.backButton);

        // כפתורים חדשים לעריכה ומחיקה
        Button editButton = view.findViewById(R.id.editAppointmentButton);
        Button deleteButton = view.findViewById(R.id.deleteAppointmentButton);
        TextView textDogName = view.findViewById(R.id.textDogName);
        Button markCompletedButton = view.findViewById(R.id.markCompletedButton);

        // בדיקת isVet מ-SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE);
        boolean isVet = prefs.getBoolean("isVet", false);

        if (getArguments() != null) {
            String date = getArguments().getString(ARG_DATE, "");
            String time = getArguments().getString(ARG_TIME, "");
            String details = getArguments().getString(ARG_DETAILS, "");
            String veterinarian = getArguments().getString(ARG_VETERINARIAN, "");
            String type = getArguments().getString(ARG_TYPE, "");
            String appointmentId = getArguments().getString(ARG_APPOINTMENT_ID, "");
            String dogId = getArguments().getString(ARG_DOG_ID, "");
            String vetId = getArguments().getString(ARG_VET_ID, "");
            String dogName = getArguments().getString(ARG_DOG_NAME, "");

            textDate.setText("Date: " + date);
            textTime.setText("Time: " + time);
            textVetName.setText("Veterinarian: " + veterinarian);

            // הצגת שם הכלב אם קיים
            if (textDogName != null) {
                if (dogName != null && !dogName.isEmpty()) {
                    textDogName.setText("Dog: " + dogName);
                    textDogName.setVisibility(View.VISIBLE);
                } else {
                    textDogName.setVisibility(View.GONE);
                }
            }

            // טיפול בסוג התור - להציג טקסט ברירת מחדל אם ריק
            if (type.isEmpty()) {
                textAppointmentType.setVisibility(View.GONE);
            } else {
                textAppointmentType.setText("Appointment type: " + type);
                textAppointmentType.setVisibility(View.VISIBLE);
            }

            // טיפול בהערות
            if (details.isEmpty()) {
                textDetails.setText("Notes: No additional notes");
            } else {
                textDetails.setText("Notes: " + details);
            }

            // הצגת כפתורי עריכה ומחיקה רק אם יש את כל הפרטים הנדרשים
            boolean canEditDelete = appointmentId != null && !appointmentId.isEmpty() &&
                    dogId != null && !dogId.isEmpty() &&
                    vetId != null && !vetId.isEmpty();

            if (editButton != null) {
                editButton.setVisibility(canEditDelete ? View.VISIBLE : View.GONE);
                editButton.setOnClickListener(v -> editAppointment(appointmentId, dogId, vetId, date));
            }

            if (deleteButton != null) {
                deleteButton.setVisibility(canEditDelete ? View.VISIBLE : View.GONE);
                deleteButton.setOnClickListener(v -> showDeleteConfirmation(appointmentId, dogId, vetId));
            }

            // הצגת כפתור "סמן כתור שהסתיים" רק לוטרינר
            if (markCompletedButton != null) {
                markCompletedButton.setVisibility(isVet ? View.VISIBLE : View.GONE);
                markCompletedButton.setOnClickListener(v -> markAppointmentCompleted(appointmentId));
            }

            if ((date == null || date.isEmpty()) && dogId != null && !dogId.isEmpty() && appointmentId != null && !appointmentId.isEmpty()) {
                // טען פרטי תור מה-DB
                FirebaseFirestore.getInstance().collection("DogProfiles").document(dogId).collection("Appointments").document(appointmentId)
                        .get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                updateUIWithAppointment(documentSnapshot);
                            }
                        });
            }
        }

        // כפתור חזרה לפרגמנט ההיסטוריה
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void editAppointment(String appointmentId, String dogId, String vetId, String date) {
        if (appointmentId == null || dogId == null || vetId == null) {
            Toast.makeText(requireContext(), "Missing appointment information", Toast.LENGTH_SHORT).show();
            return;
        }

        // פתח את מסך העריכה
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

    private void showDeleteConfirmation(String appointmentId, String dogId, String vetId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAppointment(appointmentId, dogId, vetId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAppointment(String appointmentId, String dogId, String vetId) {
        if (appointmentId == null || dogId == null || vetId == null) {
            Toast.makeText(requireContext(), "Missing appointment information for deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        // הצג דיאלוג טעינה
        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Deleting appointment...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // מחק את התור בכל המקומות
        FirestoreUserHelper.deleteAppointmentCompletely(appointmentId, dogId, vetId,
                () -> {
                    // הצלחה
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(requireContext(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show();

                    // חזור למסך הקודם (Calendar)
                    requireActivity().getSupportFragmentManager().popBackStack();
                },
                (error) -> {
                    // שגיאה
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(requireContext(), "Error deleting appointment: " + error, Toast.LENGTH_LONG).show();
                });
    }

    private void updateUIWithAppointment(DocumentSnapshot doc) {
        View view = getView();
        if (view == null) return;
        ((TextView) view.findViewById(R.id.textDate)).setText("Date: " + doc.getString("date"));
        ((TextView) view.findViewById(R.id.textTime)).setText("Time: " + doc.getString("startTime"));
        ((TextView) view.findViewById(R.id.textVetName)).setText("Veterinarian: " + doc.getString("vetName"));
        ((TextView) view.findViewById(R.id.textAppointmentType)).setText("Appointment type: " + doc.getString("type"));
        ((TextView) view.findViewById(R.id.textDogName)).setText("Dog: " + doc.getString("dogName"));
        ((TextView) view.findViewById(R.id.textDetails)).setText("Notes: " + (doc.getString("notes") == null ? "No additional notes" : doc.getString("notes")));
    }

    private void markAppointmentCompleted(String appointmentId) {
        String dogId = null;
        if (getArguments() != null) {
            dogId = getArguments().getString(ARG_DOG_ID, "");
        }
        if (appointmentId == null || appointmentId.isEmpty() || dogId == null || dogId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing appointment or dog ID", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore.getInstance().collection("DogProfiles").document(dogId).collection("Appointments").document(appointmentId)
                .update("completed", true)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Appointment marked as completed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update appointment", Toast.LENGTH_SHORT).show());
    }
}