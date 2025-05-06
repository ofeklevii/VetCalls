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
import com.example.vetcalls.vetFragment.VetHomeFragment;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;

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
                                checkUserType(currentUser.getUid()); // 🔥 Redirects user correctly
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


    private void checkUserType(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();  // יצירת אובייקט Firestore
        db.collection("Users").document(userId)  // גישה למסמך לפי userId
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // במקרה שיש מסמך, נוודא את סוג המשתמש
                            Boolean isVet = document.getBoolean("isVet");

                            if (isVet != null) {
                                // שמירת isVet ב-SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isVet", isVet);
                                editor.apply();

                                // ניווט למקום המתאים לפי סוג המשתמש
                                if (isVet) {
                                    Intent intent = new Intent(LoginActivity.this, VetHomeFragment.class);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                }
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "User data is missing 'isVet' field", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // אם אין מסמך ב-Firestore, ניצור אחד
                            createUserDocument(userId);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error getting user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserDocument(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // בודק אם ה-userID קיים אך חסר מידע
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            boolean isVet = false; // Default value is false for a regular user

            // יצירת אובייקט חדש למסמך
            com.example.vetcalls.obj.User user = new com.example.vetcalls.obj.User(email, null, null, null, null, null, null, isVet);

            db.collection("Users").document(userId)  // שמירת המשתמש במסמך Firestore
                    .set(user)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "User data created successfully!", Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isVet", isVet);
                            editor.apply();

                            // ניווט לאזור המתאים
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class); // ניווט למשתמש רגיל
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to create user data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
