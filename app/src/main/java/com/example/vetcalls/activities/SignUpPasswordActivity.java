package com.example.vetcalls.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vetcalls.R;
import com.example.vetcalls.obj.FirestoreUserHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SignUpPasswordActivity handles the final step of user registration in the VetCalls application.
 * This activity is responsible for password creation, user type selection (regular user vs. veterinarian),
 * and completing the Firebase authentication process.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Secure password creation with validation rules</li>
 *   <li>User type selection (Pet Owner vs. Veterinarian)</li>
 *   <li>Firebase Authentication account creation</li>
 *   <li>Firestore user profile initialization</li>
 *   <li>Navigation back to previous step or completion flow</li>
 * </ul>
 *
 * <p>Registration Completion Flow:</p>
 * <ol>
 *   <li>Receive email address from previous activity</li>
 *   <li>User enters secure password</li>
 *   <li>User selects account type (Pet Owner/Veterinarian)</li>
 *   <li>System validates password against security requirements</li>
 *   <li>Firebase Authentication creates user account</li>
 *   <li>Firestore user profile is initialized with selected type</li>
 *   <li>User is redirected to login screen</li>
 * </ol>
 *
 * <p>Password Security Requirements:</p>
 * <ul>
 *   <li>Must start with a capital letter</li>
 *   <li>Must contain only letters and numbers (alphanumeric)</li>
 *   <li>Must be at least 6 characters long</li>
 *   <li>No special characters or spaces allowed</li>
 * </ul>
 *
 * @author Ofek Levi
 * @version 1.0
 * @since 1.0
 * @see SignUpActivity
 * @see LoginActivity
 * @see FirestoreUserHelper
 */
public class SignUpPasswordActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private Switch vetSwitch;
    private FirebaseAuth firebaseAuth;

    /**
     * Initializes the password creation activity and sets up the user interface components.
     * This method configures password input validation, user type selection, Firebase authentication,
     * and navigation logic for completing the registration process.
     *
     * <p>Setup Process:</p>
     * <ol>
     *   <li>Initialize UI components (password input, veterinarian switch, buttons)</li>
     *   <li>Retrieve email address from previous activity intent</li>
     *   <li>Configure Firebase Authentication instance</li>
     *   <li>Set up finish button for account creation workflow</li>
     *   <li>Configure back button for return navigation</li>
     * </ol>
     *
     * <p>Account Creation Process:</p>
     * <ul>
     *   <li>Validates password against security requirements</li>
     *   <li>Creates Firebase Authentication account</li>
     *   <li>Initializes Firestore user profile with selected user type</li>
     *   <li>Provides user feedback for success or failure scenarios</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-initialized after being
     *                          previously shut down, this Bundle contains the most recent
     *                          data. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_password);

        passwordEditText = findViewById(R.id.passwordEditText);
        vetSwitch = findViewById(R.id.vetCheck);
        Button finishButton = findViewById(R.id.finishButton);
        Button backButton = findViewById(R.id.backButton);

        firebaseAuth = FirebaseAuth.getInstance();
        String email = getIntent().getStringExtra("email");

        finishButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString().trim();
            boolean isVet = vetSwitch.isChecked();

            if (isValidPassword(password)) {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {
                                    FirestoreUserHelper.createUser(user, isVet, null);
                                }
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Password must start with a capital letter, contain only letters or numbers, and be at least 6 characters long", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    /**
     * Validates the provided password against the application's security requirements.
     * This method ensures that passwords meet specific criteria for security and consistency.
     *
     * <p>Validation Criteria:</p>
     * <ul>
     *   <li><strong>First Character:</strong> Must be an uppercase letter (A-Z)</li>
     *   <li><strong>Remaining Characters:</strong> Must be letters (a-z, A-Z) or numbers (0-9)</li>
     *   <li><strong>Minimum Length:</strong> Must be at least 6 characters total</li>
     *   <li><strong>Character Restrictions:</strong> No special characters, spaces, or symbols</li>
     * </ul>
     *
     * <p>The validation uses regex pattern: ^[A-Z][a-zA-Z0-9]{5,}$</p>
     * <ul>
     *   <li>^ - Start of string</li>
     *   <li>[A-Z] - First character must be uppercase letter</li>
     *   <li>[a-zA-Z0-9]{5,} - Following 5 or more characters must be alphanumeric</li>
     *   <li>$ - End of string</li>
     * </ul>
     *
     * @param password The password string to validate
     * @return true if the password meets all security requirements, false otherwise
     */
    private boolean isValidPassword(String password) {
        return password.matches("^[A-Z][a-zA-Z0-9]{5,}$");
    }
}
