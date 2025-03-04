package com.example.vetcalls.fragment;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vetcall.R;

import java.util.Calendar;

public class AddAppointmentFragment extends Fragment {
    private EditText editTitle, editTime;
    private Spinner alertSpinner1, alertSpinner2;
    private Button btnSave;

    private String selectedDate;
    private String[] alertOptions = {"None", "During Event", "5 minutes before", "10 minutes before",
            "30 minutes before", "1 hour before", "2 hours before",
            "1 day before", "2 days before", "1 week before"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_appointment, container, false);

        editTitle = view.findViewById(R.id.editTitle);
        editTime = view.findViewById(R.id.editTime);
        alertSpinner1 = view.findViewById(R.id.alertSpinner1);
        alertSpinner2 = view.findViewById(R.id.alertSpinner2);
        btnSave = view.findViewById(R.id.btnSave);

        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, alertOptions);
        alertSpinner1.setAdapter(adapter);
        alertSpinner2.setAdapter(adapter);

        editTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveReminder());

        return view;
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            editTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void saveReminder() {
        String title = editTitle.getText().toString();
        String time = editTime.getText().toString();
        String alert1 = alertSpinner1.getSelectedItem().toString();
        String alert2 = alertSpinner2.getSelectedItem().toString();

        new AlertDialog.Builder(getContext())
                .setTitle("Reminder Saved")
                .setMessage("Your reminder for " + selectedDate + " at " + time + " has been saved.")
                .setPositiveButton("OK", (dialog, which) -> requireActivity().getSupportFragmentManager().popBackStack())
                .show();
    }
}
