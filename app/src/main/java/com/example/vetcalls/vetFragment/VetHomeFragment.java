package com.example.vetcalls.vetFragment;

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
import com.example.vetcalls.obj.PatientAdapter;
import com.example.vetcalls.obj.User;
import com.example.vetcalls.vetFragment.PatientDetailsFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VetHomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PatientAdapter adapter;
    private List<User> patientList;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewPatients);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientList = new ArrayList<>();
        adapter = new PatientAdapter(patientList, this::showPatientDetails);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        loadPatients();

        return view;
    }

    private void loadPatients() {
        firestore.collection("DogProfiles")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    patientList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        patientList.add(user);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showPatientDetails(User user) {
        // תוכל להעביר את המשתמש כ-Bundle ולפתוח Fragment חדש לצפייה בפרטים
        PatientDetailsFragment fragment = new PatientDetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putString("name", user.getName());
        bundle.putString("birthday", user.getBirthday());
        bundle.putString("vaccines", user.getVaccines());
        bundle.putString("allergies", user.getAllergies());
        bundle.putString("race", user.getRace());
        bundle.putString("weight", user.getWeight());
        bundle.putString("email", user.getEmail());

        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
