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

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private List<Map<String, Object>> appointmentList;
    private TextView emptyHistoryText;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        Log.d("HistoryDebug", "onCreateView called");

        // אתחול הרכיבים
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyHistoryText = view.findViewById(R.id.emptyHistoryText);

        // אתחול פיירבייס
        db = FirebaseFirestore.getInstance();

        // קבלת מזהה המשתמש הנוכחי
        try {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } catch (Exception e) {
            // טיפול במצב שבו אין משתמש מחובר
            showEmptyState();
            return view;
        }

        // אתחול רשימת התורים
        appointmentList = new ArrayList<>();

        // הגדרת ה-RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList, requireActivity(), false);
        recyclerView.setAdapter(adapter);

        // טעינת תורים שהסתיימו
        loadCompletedAppointments();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // רענון הרשימה כאשר הפרגמנט הופך לנראה שוב
        loadCompletedAppointments();
    }

    private void loadCompletedAppointments() {
        appointmentList.clear();
        Log.d("HistoryDebug", "Start loading completed appointments for userId: " + userId);
        db.collection("DogProfiles")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener(dogSnapshots -> {
                List<String> dogIds = new ArrayList<>();
                for (QueryDocumentSnapshot dogDoc : dogSnapshots) {
                    dogIds.add(dogDoc.getId());
                }
                Log.d("HistoryDebug", "Found " + dogIds.size() + " dogs for userId: " + userId);
                if (dogIds.isEmpty()) {
                    adapter.updateAppointments(appointmentList);
                    showEmptyState();
                    return;
                }
                final int[] finished = {0};
                final int total = dogIds.size();
                for (String dogId : dogIds) {
                    db.collection("DogProfiles")
                      .document(dogId)
                      .collection("Appointments")
                      .whereEqualTo("completed", true)
                      .get()
                      .addOnSuccessListener(appointmentsSnapshot -> {
                          Log.d("HistoryDebug", "Dog " + dogId + " has " + appointmentsSnapshot.size() + " completed appointments");
                          for (QueryDocumentSnapshot appointmentDoc : appointmentsSnapshot) {
                              Map<String, Object> appointmentData = appointmentDoc.getData();
                              Log.d("HistoryDebug", "Appointment data: " + appointmentData);
                              appointmentList.add(appointmentData);
                          }
                          finished[0]++;
                          if (finished[0] == total) {
                              Log.d("HistoryDebug", "Total completed appointments loaded: " + appointmentList.size());
                              // סינון כפילויות לפי מזהה תור
                              Map<String, Map<String, Object>> uniqueAppointments = new HashMap<>();
                              for (Map<String, Object> appt : appointmentList) {
                                  String id = (String) appt.get("id");
                                  if (id != null) uniqueAppointments.put(id, appt);
                              }
                              appointmentList = new ArrayList<>(uniqueAppointments.values());
                              adapter.updateAppointments(appointmentList);
                              if (appointmentList.isEmpty()) showEmptyState();
                              else hideEmptyState();
                          }
                      })
                      .addOnFailureListener(e -> {
                          Log.e("HistoryDebug", "Failed to load appointments for dog " + dogId, e);
                          finished[0]++;
                          if (finished[0] == total) {
                              Log.d("HistoryDebug", "Total completed appointments loaded (with errors): " + appointmentList.size());
                              adapter.updateAppointments(appointmentList);
                              if (appointmentList.isEmpty()) showEmptyState();
                              else hideEmptyState();
                          }
                      });
                }
            });
    }

    private void showEmptyState() {
        if (emptyHistoryText != null) {
            emptyHistoryText.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyHistoryText != null) {
            emptyHistoryText.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}