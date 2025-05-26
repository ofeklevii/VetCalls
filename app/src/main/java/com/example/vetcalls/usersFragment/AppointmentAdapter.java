package com.example.vetcalls.usersFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;

import java.util.List;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {
    private List<Map<String, Object>> appointmentList;
    private FragmentActivity activity;

    public AppointmentAdapter(List<Map<String, Object>> appointmentList, FragmentActivity activity) {
        this.appointmentList = appointmentList;
        this.activity = activity;
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
            AppointmentDetailsFragment detailsFragment = AppointmentDetailsFragment.newInstanceFull(
                    appointment.get("date") != null ? appointment.get("date").toString() : "",
                    appointment.get("startTime") != null ? appointment.get("startTime").toString() : "",
                    appointment.get("notes") != null ? appointment.get("notes").toString() : "",
                    appointment.get("vetName") != null ? appointment.get("vetName").toString() : "",
                    appointment.get("type") != null ? appointment.get("type").toString() : "",
                    appointment.get("id") != null ? appointment.get("id").toString() : "",
                    appointment.get("dogId") != null ? appointment.get("dogId").toString() : "",
                    appointment.get("vetId") != null ? appointment.get("vetId").toString() : "",
                    appointment.get("dogName") != null ? appointment.get("dogName").toString() : ""
            );

            // החלפת הפרגמנט הנוכחי בפרגמנט הפרטים
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
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