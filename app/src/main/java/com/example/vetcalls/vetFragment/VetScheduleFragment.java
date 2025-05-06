package com.example.vetcalls.vetFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.vetcalls.R;
import com.example.vetcalls.usersFragment.AddAppointmentFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class VetScheduleFragment extends Fragment {

    private CalendarView calendarView;
    private LinearLayout appointmentsContainer;
    private FirebaseFirestore db;

    private String selectedDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        appointmentsContainer = view.findViewById(R.id.appointmentsContainer); // This should be added to fragment_calendar.xml

        db = FirebaseFirestore.getInstance();

        selectedDate = getTodayDateString(); // show today's appointments by default
        loadAppointments(selectedDate);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            openAddReminderFragment(selectedDate);
        });

        return view;
    }

    private void openAddReminderFragment(String date) {
        Bundle bundle = new Bundle();
        bundle.putString("selectedDate", date);

        AddAppointmentFragment addAppointmentFragment = new AddAppointmentFragment();
        addAppointmentFragment.setArguments(bundle);

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, addAppointmentFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void loadAppointments(String date) {
        CollectionReference appointmentsRef = db.collection("Appointments");

        appointmentsRef
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    appointmentsContainer.removeAllViews(); // Clear previous entries
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String time = document.getString("time");
                        String type = document.getString("type");

                        TextView appointmentView = new TextView(getContext());
                        appointmentView.setText("• " + time + " - " + type);
                        appointmentView.setTextSize(16);
                        appointmentsContainer.addView(appointmentView);
                    }

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("No appointments for this date.");
                        emptyView.setTextSize(16);
                        appointmentsContainer.addView(emptyView);
                    }
                });
    }

    private String getTodayDateString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!selectedDate.isEmpty()) {
            loadAppointments(selectedDate);
        }
    }
}
