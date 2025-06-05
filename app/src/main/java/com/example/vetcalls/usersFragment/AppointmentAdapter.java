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

/**
 * RecyclerView adapter for displaying appointment items in a list.
 * Handles the binding of appointment data to view holders and manages navigation
 * to appointment details when items are clicked.
 *
 * @author Ofek Levi
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {

    /** List of appointment data maps */
    private List<Map<String, Object>> appointmentList;

    /** Fragment activity for navigation purposes */
    private FragmentActivity activity;

    /** Whether to show action buttons in appointment details */
    private boolean showActions;

    /**
     * Constructor for creating the appointment adapter.
     *
     * @param appointmentList List of appointment data maps to display
     * @param activity Fragment activity for navigation
     * @param showActions Whether to show action buttons in appointment details
     */
    public AppointmentAdapter(List<Map<String, Object>> appointmentList, FragmentActivity activity, boolean showActions) {
        this.appointmentList = appointmentList;
        this.activity = activity;
        this.showActions = showActions;
    }

    /**
     * Creates a new ViewHolder by inflating the appointment item layout.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new AppointmentViewHolder that holds a View for the appointment item
     */
    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    /**
     * Binds appointment data to the ViewHolder at the specified position.
     * Sets up the appointment information display and click listener for navigation.
     *
     * @param holder The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Map<String, Object> appointment = appointmentList.get(position);

        holder.dateTextView.setText("Date: " + (appointment.get("date") != null ? appointment.get("date").toString() : ""));
        holder.timeTextView.setText("Time: " + (appointment.get("startTime") != null ? appointment.get("startTime").toString() : ""));
        holder.dogNameTextView.setText("Dog: " + (appointment.get("dogName") != null ? appointment.get("dogName").toString() : ""));

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

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of appointments, or 0 if the list is null
     */
    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    /**
     * Updates the appointment list with new data and refreshes the RecyclerView.
     *
     * @param appointments The new list of appointment data maps
     */
    public void updateAppointments(List<Map<String, Object>> appointments) {
        this.appointmentList = appointments;
        notifyDataSetChanged();
    }
}