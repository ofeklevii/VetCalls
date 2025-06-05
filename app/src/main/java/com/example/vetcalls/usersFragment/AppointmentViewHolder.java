package com.example.vetcalls.usersFragment;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;

/**
 * ViewHolder class for appointment items in the RecyclerView.
 * Holds references to the UI components of each appointment item
 * to enable efficient data binding and view recycling.
 *
 * @author Ofek Levi
 */
public class AppointmentViewHolder extends RecyclerView.ViewHolder {

    /** TextView for displaying the appointment date */
    TextView dateTextView;

    /** TextView for displaying the appointment time */
    TextView timeTextView;

    /** TextView for displaying the dog's name */
    TextView dogNameTextView;

    /**
     * Constructor that initializes the ViewHolder with the given item view.
     * Finds and stores references to the UI components within the appointment item layout.
     *
     * @param itemView The root view of the appointment item layout
     */
    public AppointmentViewHolder(@NonNull View itemView) {
        super(itemView);
        dateTextView = itemView.findViewById(R.id.textAppointmentDate);
        timeTextView = itemView.findViewById(R.id.textAppointmentTime);
        dogNameTextView = itemView.findViewById(R.id.textDogName);
    }
}