package com.example.vetcalls.usersFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;

import java.util.List;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
    private List<Map<String, Object>> appointmentList;
    private FragmentActivity activity;

    public AppointmentAdapter(List<Map<String, Object>> appointmentList, FragmentActivity activity) {
        this.appointmentList = appointmentList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> appointment = appointmentList.get(position);

        // תצוגת תאריך ושעה
        holder.dateTextView.setText("תאריך: " + (appointment.get("date") != null ? appointment.get("date").toString() : ""));
        holder.timeTextView.setText("שעה: " + (appointment.get("time") != null ? appointment.get("time").toString() : ""));

        // פתיחת מסך פרטים בלחיצה
        holder.itemView.setOnClickListener(v -> {
            // יצירת פרגמנט פרטי תור עם כל המידע הנדרש
            AppointmentDetailsFragment detailsFragment = AppointmentDetailsFragment.newInstance(
                    appointment.get("date") != null ? appointment.get("date").toString() : "",
                    appointment.get("time") != null ? appointment.get("time").toString() : "",
                    appointment.get("details") != null ? appointment.get("details").toString() : "",
                    appointment.get("veterinarian") != null ? appointment.get("veterinarian").toString() : "",
                    appointment.get("type") != null ? appointment.get("type").toString() : ""
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView, timeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.textAppointmentDate);
            timeTextView = itemView.findViewById(R.id.textAppointmentTime);
        }
    }
}