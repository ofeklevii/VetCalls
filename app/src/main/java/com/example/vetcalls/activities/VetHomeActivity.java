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

/**
 * Main activity for veterinarian users providing navigation between different sections.
 * Manages the bottom navigation view and fragment transitions for the veterinarian interface.
 *
 * @author Ofek Levi
 */
public class VetHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    /**
     * Called when the activity is first created.
     * Initializes the UI components and sets up the bottom navigation functionality.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            /**
             * Called when an item in the bottom navigation is selected.
             *
             * @param item The selected menu item
             * @return true if the item selection was handled, false otherwise
             */
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

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new VetHomeFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_home1);
        }
    }
}