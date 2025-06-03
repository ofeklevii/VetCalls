package com.example.vetcalls.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vetcalls.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * LoginActivity serves as the authentication entry point for the VetCalls application.
 * This activity handles user authentication, user type determination, and routing users
 * to the appropriate interface based on their account type (Pet Owner or Veterinarian).
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Firebase Authentication integration for secure login</li>
 *   <li>User type detection (Pet Owner vs. Veterinarian)</li>
 *   <li>Automatic routing to appropriate home interface</li>
 *   <li>User document creation for new authenticated users</li>
 *   <li>SharedPreferences integration for user type caching</li>
 *   <li>Navigation to registration and password recovery flows</li>
 * </ul>
 *
 * <p>Authentication Flow:</p>
 * <ol>
 *   <li>User enters email and password credentials</li>
 *   <li>Firebase Authentication validates credentials</li>
 *   <li>System checks Firestore for user type information</li>
 *   <li>User type is cached in SharedPreferences</li>
 *   <li>User is routed to appropriate home activity</li>
 *   <li>If user document doesn't exist, it's created with default values</li>
 * </ol>
 *
 * <p>User Type Routing:</p>
 * <ul>
 *   <li><strong>Pet Owners (isVet = false):</strong> Routed to HomeActivity</li>
 *   <li><strong>Veterinarians (isVet = true):</strong> Routed to VetHomeActivity</li>
 *   <li><strong>Unknown/Default:</strong> Treated as Pet Owner and routed to HomeActivity</li>
 * </ul>
 *
 * @author Ofek Levi
 * @version 1.0
 * @since 1.0
 * @see HomeActivity
 * @see VetHomeActivity
 * @see SignUpActivity
 * @see ForgotPasswordActivity
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;

    /**
     * Initializes the login activity and sets up the authentication interface.
     * This method configures all UI components, Firebase authentication,
     * and navigation handlers for the login process.
     *
     * <p>Setup Process:</p>
     * <ol>
     *   <li>Initialize UI components (email input, password input, buttons)</li>
     *   <li>Configure Firebase Authentication instance</li>
     *   <li>Set up SharedPreferences for user data caching</li>
     *   <li>Configure login button with authentication logic</li>
     *   <li>Set up navigation to forgot password and sign-up flows</li>
     * </ol>
     *
     * <p>Input Validation:</p>
     * The login process includes validation for:
     * <ul>
     *   <li>Non-empty email and password fields</li>
     *   <li>Firebase Authentication credential verification</li>
     *   <li>User existence and type validation in Firestore</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-initialized after being
     *                          previously shut down, this Bundle contains the most recent
     *                          data. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailEditText = findViewById(R.id.emailInput);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        TextView signUpTextView = findViewById(R.id.signUpTextView);
        TextView forgotPasswordTextView = findViewById(R.id.forgotPassword);

        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);

        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                            if (currentUser != null) {
                                checkUserType(currentUser.getUid());
                            }
                        } else {
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        signUpTextView.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Checks the authenticated user's type in Firestore and routes them accordingly.
     * This method queries the user's document to determine if they are a veterinarian
     * or pet owner, caches this information locally, and navigates to the appropriate interface.
     *
     * <p>User Type Determination Process:</p>
     * <ol>
     *   <li>Query Firestore Users collection for user document</li>
     *   <li>Extract isVet boolean field from user document</li>
     *   <li>Cache user type in SharedPreferences</li>
     *   <li>Route to VetHomeActivity (if veterinarian) or HomeActivity (if pet owner)</li>
     *   <li>Handle missing user documents by creating default user profile</li>
     * </ol>
     *
     * <p>Fallback Behavior:</p>
     * <ul>
     *   <li>If isVet field is null or missing, defaults to false (pet owner)</li>
     *   <li>If user document doesn't exist, calls createUserDocument() method</li>
     *   <li>All navigation includes activity finish() to prevent back navigation to login</li>
     * </ul>
     *
     * @param userId The unique Firebase Authentication user ID
     */
    private void checkUserType(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Boolean isVet = document.getBoolean("isVet");

                            if (isVet != null) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isVet", isVet);
                                editor.apply();

                                if (isVet) {
                                    Intent intent = new Intent(LoginActivity.this, VetHomeActivity.class);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                }
                                finish();
                            } else {
                                boolean defaultIsVet = false;

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isVet", defaultIsVet);
                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            createUserDocument(userId);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error getting user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates a new user document in Firestore for authenticated users without existing profiles.
     * This method handles the creation of default user profiles for users who have successfully
     * authenticated but don't have corresponding user documents in the database.
     *
     * <p>User Document Creation Process:</p>
     * <ol>
     *   <li>Extract email from authenticated Firebase user</li>
     *   <li>Set default user type as pet owner (isVet = false)</li>
     *   <li>Create User object with email, user type, and user ID</li>
     *   <li>Save user document to Firestore Users collection</li>
     *   <li>Cache user type in SharedPreferences</li>
     *   <li>Navigate to HomeActivity upon successful creation</li>
     * </ol>
     *
     * <p>Default Values:</p>
     * <ul>
     *   <li><strong>isVet:</strong> false (defaults to pet owner)</li>
     *   <li><strong>email:</strong> Retrieved from Firebase Authentication</li>
     *   <li><strong>userId:</strong> Firebase Authentication UID</li>
     * </ul>
     *
     * <p>This method ensures that all authenticated users have corresponding
     * user profiles in Firestore for proper application functionality.</p>
     *
     * @param userId The unique Firebase Authentication user ID for the new user document
     */
    private void createUserDocument(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            boolean isVet = false;

            com.example.vetcalls.obj.User user = new com.example.vetcalls.obj.User(
                    email, isVet, userId);

            db.collection("Users").document(userId)
                    .set(user)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "User data created successfully!", Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isVet", isVet);
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to create user data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}