package com.example.vetcalls.usersFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.Appointment;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Dummy data for testing
        appointmentList = new ArrayList<>();
        appointmentList.add(new Appointment("2024-02-01", "10:30 AM", "Regular check-up, weight: 15kg", "Shon Aronov"));
        appointmentList.add(new Appointment("2024-01-20", "02:00 PM", "Vaccination for rabies", "Ofek Levi"));
        appointmentList.add(new Appointment("2023-12-05", "04:15 PM", "Ear infection treatment", "Emily Panfilov"));

        adapter = new AppointmentAdapter(appointmentList, requireActivity());
        recyclerView.setAdapter(adapter);

        return view;
    }
}

