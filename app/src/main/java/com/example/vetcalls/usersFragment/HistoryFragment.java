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
        adapter = new AppointmentAdapter(appointmentList, requireActivity());
        recyclerView.setAdapter(adapter);

        // טעינת תורים שהסתיימו
        loadCompletedAppointments();

        Toast.makeText(getContext(), "HistoryFragment loaded", Toast.LENGTH_SHORT).show();

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

        db.collection("appointments")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HistoryDebug", "userId=" + userId);
                    Log.d("HistoryDebug", "query size=" + queryDocumentSnapshots.size());
                    List<Map<String, Object>> pastAppointments = new ArrayList<>();
                    Date now = new Date();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String date = (String) document.get("date");
                        String endTime = (String) document.get("endTime");
                        Boolean completed = (Boolean) document.get("completed");
                        Log.d("HistoryDebug", "date=" + date + ", endTime=" + endTime);
                        if (date != null && endTime != null) {
                            try {
                                Date appointmentEnd = format.parse(date + " " + endTime);
                                Log.d("HistoryDebug", "appointmentEnd=" + appointmentEnd + ", now=" + now);
                                if ((appointmentEnd != null && appointmentEnd.before(now)) || (completed != null && completed)) {
                                    pastAppointments.add(document.getData());
                                    Log.d("HistoryDebug", "ADDED: " + document.getId());
                                } else {
                                    Log.d("HistoryDebug", "NOT ADDED: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e("HistoryDebug", "Parse error: " + e.getMessage());
                            }
                        }
                    }

                    // סדר יורד לפי תאריך+שעה
                    pastAppointments.sort((a, b) -> {
                        try {
                            Date da = format.parse(a.get("date") + " " + a.get("endTime"));
                            Date db_ = format.parse(b.get("date") + " " + b.get("endTime"));
                            return db_.compareTo(da);
                        } catch (Exception e) { return 0; }
                    });

                    appointmentList.addAll(pastAppointments);
                    adapter.updateAppointments(appointmentList);

                    if (appointmentList.isEmpty()) showEmptyState();
                    else hideEmptyState();
                })
                .addOnFailureListener(e -> showEmptyState());
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