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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initViews(view);
        initData();
        setupCalendar();
        setupAddButton();

        // טען תורים לתאריך הנוכחי
        selectedDate = getTodayDateString();
        loadAppointments(selectedDate);

        return view;
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView);
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton);

        // אתחול RecyclerView
        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, requireActivity(), true);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentsRecyclerView.setAdapter(appointmentAdapter);
    }

    private void initData() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        isVet = sharedPreferences.getBoolean("isVet", false);

        Log.d(TAG, "User initialized - userId: " + userId + ", isVet: " + isVet);
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // ודא פורמט תואם ל-AddAppointmentFragment
            selectedDate = String.format(Locale.getDefault(), "%d-%d-%d", year, month + 1, dayOfMonth);
            Log.d(TAG, "Date selected: " + selectedDate);
            loadAppointments(selectedDate);
        });
    }

    private void setupAddButton() {
        if (isVet) {
            addAppointmentButton.setText("Add appointment for patient");
        } else {
            addAppointmentButton.setText("Make an appointment");
        }
        addAppointmentButton.setOnClickListener(v -> openAddAppointmentFragment());
    }

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
                        appointmentData.put("documentId", document.getId()); // הוסף ID למחיקה/עריכה
                        boolean isCompleted = appointmentData.get("completed") instanceof Boolean && (Boolean) appointmentData.get("completed");
                        String apptDate = (String) appointmentData.get("date");
                        String apptTime = (String) appointmentData.get("startTime");
                        if (!isCompleted) {
                            // אם השעה עברה, עדכן ל-completed בפיירסטור
                            if (!isFutureAppointment(apptDate, apptTime)) {
                                markAppointmentCompletedForVet(document.getId(), (String)appointmentData.get("dogId"), userId);
                                continue; // לא להציג תורים שהסתיימו
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

    private boolean shouldShowAppointment(String date, String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
            Date appointmentDate = sdf.parse(date);
            Calendar apptCal = Calendar.getInstance();
            apptCal.setTime(appointmentDate);
            Calendar now = Calendar.getInstance();
            // אם התור היום
            if (apptCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                apptCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                apptCal.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                return true;
            }
            // אם התור בעתיד
            return appointmentDate != null && appointmentDate.after(now.getTime());
        } catch (Exception e) {
            return true; // אם יש שגיאה, תציג ליתר ביטחון
        }
    }

    private boolean isFutureAppointment(String date, String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault());
            Date appointmentDate = sdf.parse(date + " " + startTime);
            return appointmentDate != null && appointmentDate.after(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private void markAppointmentCompletedEverywhere(String appointmentId, String dogId, String vetId) {
        // עדכון אצל הווטרינר
        db.collection("Veterinarians")
          .document(vetId)
          .collection("Appointments")
          .document(appointmentId)
          .update("completed", true)
          .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment marked as completed for vet: " + appointmentId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to update appointment for vet: " + appointmentId, e));

        // עדכון אצל הכלב
        db.collection("DogProfiles")
          .document(dogId)
          .collection("Appointments")
          .document(appointmentId)
          .update("completed", true)
          .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment marked as completed for dog: " + appointmentId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to update appointment for dog: " + appointmentId, e));
    }

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

    private void updateUI() {
        appointmentAdapter.notifyDataSetChanged();

        if (appointmentList.isEmpty()) {
            showEmptyView("No appointments for " + selectedDate);
        } else {
            showAppointmentsView();
            Log.d(TAG, "UI updated with " + appointmentList.size() + " appointments");
        }
    }

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

    private String getTodayDateString() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
        String today = dateFormat.format(calendar.getTime());
        Log.d(TAG, "Today's date: " + today);
        return today;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // רענן את הנתונים כשחוזרים למסך
        if (!selectedDate.isEmpty()) {
            Log.d(TAG, "Refreshing appointments for: " + selectedDate);
            loadAppointments(selectedDate);
        } else {
            selectedDate = getTodayDateString();
            loadAppointments(selectedDate);
        }
    }

}