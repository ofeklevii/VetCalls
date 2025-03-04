package com.example.vetcalls.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vetcall.R;

public class AppointmentDetailsFragment extends Fragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";
    private static final String ARG_DETAILS = "details";
    private static final String ARG_DOCTOR= "doctor";

    public static AppointmentDetailsFragment newInstance(String date, String time, String details, String doctor) {
        AppointmentDetailsFragment fragment = new AppointmentDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        args.putString(ARG_TIME, time);
        args.putString(ARG_DETAILS, details);
        args.putString(ARG_DOCTOR, doctor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_details, container, false);

        TextView textDate = view.findViewById(R.id.textDate);
        TextView textTime = view.findViewById(R.id.textTime);
        TextView textDetails = view.findViewById(R.id.textDetails);
        TextView textVetName = view.findViewById(R.id.textVetName);
        ImageView backButton = view.findViewById(R.id.backButton);

        if (getArguments() != null) {
            textDate.setText(getArguments().getString(ARG_DATE));
            textTime.setText(getArguments().getString(ARG_TIME));
            textDetails.setText(getArguments().getString(ARG_DETAILS));
            textVetName.setText(getArguments().getString(ARG_DOCTOR));
        }

        // Back button to return to HistoryFragment
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }
}
