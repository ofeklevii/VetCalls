package com.example.vetcalls.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.vetcalls.R;
import com.example.vetcalls.usersFragment.CalendarFragment;
import com.example.vetcalls.usersFragment.ChatFragment;
import com.example.vetcalls.usersFragment.HistoryFragment;
import com.example.vetcalls.usersFragment.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * HomeActivity serves as the main container activity for regular users in the VetCalls application.
 * This activity manages the bottom navigation system and coordinates between different user-facing fragments.
 *
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Initialize and manage the bottom navigation interface</li>
 *   <li>Handle fragment transactions between main user sections</li>
 *   <li>Provide navigation between Home, Chat, History, and Calendar views</li>
 *   <li>Set up the default landing fragment (HomeFragment)</li>
 * </ul>
 *
 * <p>Navigation Structure:</p>
 * <ul>
 *   <li><strong>Home:</strong> Main dashboard with dog profiles and user information</li>
 *   <li><strong>Chat:</strong> Messaging interface for veterinarian communication</li>
 *   <li><strong>History:</strong> Appointment history and completed visits</li>
 *   <li><strong>Calendar:</strong> Appointment scheduling and calendar management</li>
 * </ul>
 *
 * <p>This activity is specifically designed for regular users (pet owners) and uses
 * the bottom_nav_menu resource for navigation items. For veterinarian users,
 * a separate activity (VetHomeActivity) should be used.</p>
 *
 * @author Ofek Levi
 * @version 1.0
 * @since 1.0
 * @see VetHomeActivity
 * @see HomeFragment
 * @see ChatFragment
 * @see HistoryFragment
 * @see CalendarFragment
 */
public class HomeActivity extends AppCompatActivity {

    /**
     * Initializes the activity, sets up the user interface, and configures navigation.
     * This method establishes the fragment container, loads the default fragment,
     * and configures the bottom navigation with appropriate menu items and listeners.
     *
     * <p>Initialization Process:</p>
     * <ol>
     *   <li>Set the activity layout (activity_home)</li>
     *   <li>Load HomeFragment as the default landing page</li>
     *   <li>Initialize bottom navigation with user-specific menu</li>
     *   <li>Set up navigation item selection listener</li>
     * </ol>
     *
     * @param savedInstanceState If the activity is being re-initialized after being
     *                          previously shut down, this Bundle contains the most recent
     *                          data. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Fragment defaultFragment = new HomeFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, defaultFragment)
                .commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.getMenu().clear();
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            /**
             * Handles navigation item selection and performs fragment transitions.
             * This method determines which fragment to display based on the selected menu item
             * and manages the fragment transaction to update the UI accordingly.
             *
             * <p>Supported Navigation Items:</p>
             * <ul>
             *   <li><strong>nav_home:</strong> Displays HomeFragment with user dashboard</li>
             *   <li><strong>nav_chat:</strong> Displays ChatFragment for messaging</li>
             *   <li><strong>nav_history:</strong> Displays HistoryFragment with appointment history</li>
             *   <li><strong>nav_calendar:</strong> Displays CalendarFragment for scheduling</li>
             * </ul>
             *
             * @param item The selected menu item from the bottom navigation
             * @return true if the navigation was handled successfully, false otherwise
             */
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
