package com.example.vetcalls.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.vetcall.fragment.CalendarFragment;
import com.example.vetcall.fragment.ChatFragment;
import com.example.vetcall.fragment.HistoryFragment;
import com.example.vetcall.fragment.HomeFragment;
import com.example.vetcall.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private Button editProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Default fragment: HomeFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();



        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.nav_chat) {
                    selectedFragment = new ChatFragment();
                } else if (item.getItemId() == R.id.nav_history) {
                    selectedFragment = new HistoryFragment();
                } else if (item.getItemId() == R.id.nav_calendar) {
                    selectedFragment = new CalendarFragment();
                }


                // Replace current fragment with the selected one
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }

                return true;
            }
        });
    }
}


