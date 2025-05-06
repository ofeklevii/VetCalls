package com.example.vetcalls.usersFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;
import com.example.vetcalls.obj.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
    private List<Appointment> appointmentList;
    private FragmentActivity activity;

    public AppointmentAdapter(List<Appointment> appointmentList, FragmentActivity activity) {
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
        Appointment appointment = appointmentList.get(position);
        holder.dateTextView.setText(appointment.getDate());
        holder.timeTextView.setText(appointment.getTime());

        // Open details when clicked
        holder.itemView.setOnClickListener(v -> {
            AppointmentDetailsFragment detailsFragment = AppointmentDetailsFragment.newInstance(
                    appointment.getDate(), appointment.getTime(), appointment.getDetails(), appointment.getVeterinarian());

            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
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
