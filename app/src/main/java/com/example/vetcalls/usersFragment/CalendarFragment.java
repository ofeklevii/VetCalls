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
        appointmentAdapter = new AppointmentAdapter(appointmentList, requireActivity());
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
                        appointmentList.add(appointmentData);
                        Log.d(TAG, "Added vet appointment: " + appointmentData.get("type") + " at " + appointmentData.get("startTime"));
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

        // קודם טען את רשימת הכלבים
        db.collection("Users")
                .document(userId)
                .collection("Dogs")
                .get()
                .addOnSuccessListener(dogSnapshots -> {
                    if (dogSnapshots.isEmpty()) {
                        Log.d(TAG, "No dogs found for user");
                        updateUI();
                        return;
                    }

                    Log.d(TAG, "Found " + dogSnapshots.size() + " dogs for user");
                    List<Task<QuerySnapshot>> appointmentTasks = new ArrayList<>();

                    // צור משימה לכל כלב
                    for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                        String dogId = dogDoc.getString("dogId");
                        String dogName = dogDoc.getString("name");

                        if (dogId != null) {
                            Log.d(TAG, "Loading appointments for dog: " + dogName + " (ID: " + dogId + ")");

                            Task<QuerySnapshot> appointmentTask = db.collection("DogProfiles")
                                    .document(dogId)
                                    .collection("Appointments")
                                    .whereEqualTo("date", date)
                                    .get();
                            appointmentTasks.add(appointmentTask);
                        }
                    }

                    // חכה שכל המשימות יסתיימו
                    Tasks.whenAllComplete(appointmentTasks)
                            .addOnCompleteListener(allTasks -> {
                                appointmentList.clear();

                                int taskIndex = 0;
                                for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                                    String dogName = dogDoc.getString("name");

                                    if (taskIndex < allTasks.getResult().size()) {
                                        Task<?> task = allTasks.getResult().get(taskIndex);

                                        if (task.isSuccessful()) {
                                            QuerySnapshot appointmentSnapshot = (QuerySnapshot) task.getResult();
                                            Log.d(TAG, "Found " + appointmentSnapshot.size() + " appointments for " + dogName);

                                            for (QueryDocumentSnapshot appointmentDoc : appointmentSnapshot) {
                                                Map<String, Object> appointmentData = appointmentDoc.getData();
                                                appointmentData.put("dogName", dogName);
                                                appointmentData.put("documentId", appointmentDoc.getId());
                                                appointmentList.add(appointmentData);
                                                Log.d(TAG, "Added patient appointment: " + appointmentData.get("type") + " for " + dogName);
                                            }
                                        } else {
                                            Log.e(TAG, "Error loading appointments for " + dogName, task.getException());
                                        }
                                    }
                                    taskIndex++;
                                }

                                Log.d(TAG, "Total appointments loaded: " + appointmentList.size());
                                updateUI();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user dogs", e);
                    Toast.makeText(getContext(), "Error loading dogs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI();
                });
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

    // פונקציה לרענון ידני (אופציונלית)
    public void refreshAppointments() {
        if (!selectedDate.isEmpty()) {
            Log.d(TAG, "Manual refresh requested");
            loadAppointments(selectedDate);
        }
    }
}