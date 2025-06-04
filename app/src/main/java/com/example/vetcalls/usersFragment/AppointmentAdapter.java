package com.example.vetcalls.usersFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;

import java.util.List;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {
    private List<Map<String, Object>> appointmentList;
    private FragmentActivity activity;
    private boolean showActions;

    public AppointmentAdapter(List<Map<String, Object>> appointmentList, FragmentActivity activity, boolean showActions) {
        this.appointmentList = appointmentList;
        this.activity = activity;
        this.showActions = showActions;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Map<String, Object> appointment = appointmentList.get(position);

        // תצוגת תאריך ושעה
        holder.dateTextView.setText("Date: " + (appointment.get("date") != null ? appointment.get("date").toString() : ""));
        holder.timeTextView.setText("Time: " + (appointment.get("startTime") != null ? appointment.get("startTime").toString() : ""));
        holder.dogNameTextView.setText("Dog: " + (appointment.get("dogName") != null ? appointment.get("dogName").toString() : ""));

        // פתיחת מסך פרטים בלחיצה עם כל הפרטים הנדרשים
        holder.itemView.setOnClickListener(v -> {
            AppointmentDetailsFragment detailsFragment = new AppointmentDetailsFragment();
            Bundle args = new Bundle();
            args.putString("appointmentId", (String) appointment.get("id"));
            args.putString("dogId", (String) appointment.get("dogId"));
            args.putString("vetId", (String) appointment.get("vetId"));
            args.putBoolean("showActions", showActions);
            detailsFragment.setArguments(args);

            FragmentActivity activity = (FragmentActivity) holder.itemView.getContext();
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, detailsFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    // עדכון רשימת התורים כשיש מידע חדש
    public void updateAppointments(List<Map<String, Object>> appointments) {
        this.appointmentList = appointments;
        notifyDataSetChanged();
    }
}