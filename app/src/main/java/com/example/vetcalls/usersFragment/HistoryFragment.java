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

import com.example.vetcalls.R;
import com.example.vetcalls.obj.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // רענון הרשימה כאשר הפרגמנט הופך לנראה שוב
        loadCompletedAppointments();
    }

    private void loadCompletedAppointments() {
        // ניקוי הרשימה הנוכחית
        appointmentList.clear();

        // שאילתה לפיירבייס לתורים שהושלמו
        // התאם את המסלול לאוסף בהתאם למבנה ה-Firebase שלך
        db.collection("appointments")
                .whereEqualTo("completed", true)  // רק תורים שהושלמו
                .whereEqualTo("patientId", userId)  // רק תורים של המשתמש הנוכחי
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // לא נמצאו תורים שהושלמו
                        showEmptyState();
                    } else {
                        // עיבוד התורים
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            appointmentList.add(document.getData());
                        }

                        // עדכון האדפטר
                        adapter.updateAppointments(appointmentList);

                        // הסתרת מצב ריק אם יש תורים
                        hideEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    // טיפול בשגיאות
                    showEmptyState();
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