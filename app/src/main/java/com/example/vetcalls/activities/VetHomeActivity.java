package com.example.vetcalls.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.vetcalls.R;
import com.example.vetcalls.vetFragment.VetHomeFragment;
import com.example.vetcalls.usersFragment.CalendarFragment;
import com.example.vetcalls.usersFragment.ChatFragment;
import com.example.vetcalls.vetFragment.PatientDetailsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class VetHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // הגדרת האזנה לשינוי בתפריט הניווט
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home1) {
                    selectedFragment = new VetHomeFragment();
                } else if (itemId == R.id.nav_chats) {
                    selectedFragment = new ChatFragment();
                } else if (itemId == R.id.nav_patient) {
                    selectedFragment = new PatientDetailsFragment();
                } else if (itemId == R.id.nav_schedule) {
                    selectedFragment = new CalendarFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });

        // טען את VetHomeFragment כברירת מחדל
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new VetHomeFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_home1); // סמן את הכפתור הראשון כנבחר
        }
    }
}