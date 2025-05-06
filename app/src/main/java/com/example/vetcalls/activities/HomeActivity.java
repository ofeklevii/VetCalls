package com.example.vetcalls.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.vetcalls.R;
import com.example.vetcalls.vetFragment.VetHomeFragment;
import com.example.vetcalls.vetFragment.VetScheduleFragment;
import com.example.vetcalls.usersFragment.CalendarFragment;
import com.example.vetcalls.usersFragment.ChatFragment;
import com.example.vetcalls.usersFragment.HistoryFragment;
import com.example.vetcalls.usersFragment.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get isVet value from SharedPreferences (set during login or profile update)
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        boolean isVet = sharedPreferences.getBoolean("isVet", false);

        // Set the default fragment based on the user role.
        Fragment defaultFragment;
        if (isVet) {
            defaultFragment = new VetHomeFragment();
        } else {
            defaultFragment = new HomeFragment();
        }
        // Load the default fragment into your container
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, defaultFragment)
                .commit();

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Load different menu items based on whether the user is a veterinarian or not.
        if (isVet) {
            bottomNavigationView.getMenu().clear(); // Clear any existing menu
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_doc); // Menu for doctors
        } else {
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu); // Menu for regular users
        }

        // Handle BottomNavigation item selections:
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (isVet) {
                    // veterinarian's navigation: adjust menu IDs and fragments as required.
                    if (item.getItemId() == R.id.nav_home) {
                        selectedFragment = new VetHomeFragment();
                    } else if (item.getItemId() == R.id.nav_schedule) {
                        selectedFragment = new VetScheduleFragment();
                    } else if (item.getItemId() == R.id.nav_chats) {
                        selectedFragment = new ChatFragment();
                    }
                } else {
                    // Regular user's navigation
                    if (item.getItemId() == R.id.nav_home) {
                        selectedFragment = new HomeFragment();
                    } else if (item.getItemId() == R.id.nav_chat) {
                        selectedFragment = new ChatFragment();
                    } else if (item.getItemId() == R.id.nav_history) {
                        selectedFragment = new HistoryFragment();
                    } else if (item.getItemId() == R.id.nav_calendar) {
                        selectedFragment = new CalendarFragment();
                    }
                }

                if (selectedFragment != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, selectedFragment);
                    transaction.commit();
                }
                return true;
            }
        });
    }
}
