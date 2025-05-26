// AppointmentViewHolder.java
package com.example.vetcalls.usersFragment;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vetcalls.R;

public class AppointmentViewHolder extends RecyclerView.ViewHolder {
    TextView dateTextView, timeTextView, dogNameTextView;

    public AppointmentViewHolder(@NonNull View itemView) {
        super(itemView);
        dateTextView = itemView.findViewById(R.id.textAppointmentDate);
        timeTextView = itemView.findViewById(R.id.textAppointmentTime);
        dogNameTextView = itemView.findViewById(R.id.textDogName);
    }
}