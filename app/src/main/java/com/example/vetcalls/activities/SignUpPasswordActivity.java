package com.example.vetcalls.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vetcall.R;
import com.example.vetcall.obj.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpPasswordActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_password);

        passwordEditText = findViewById(R.id.passwordEditText);
        Button finishButton = findViewById(R.id.finishButton);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        String email = getIntent().getStringExtra("email");

        finishButton.setOnClickListener(view -> {
            String password = passwordEditText.getText().toString().trim();

            if (isValidPassword(password)) {
                // Create the user in Firebase Authentication
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();

                                // Save additional user data in Firebase Database
                                if (user != null) {
                                    saveToDatabase(user.getUid(), email);
                                }

                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                                // Redirect to login page
                                Intent intent = new Intent(SignUpPasswordActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Password must start with a capital letter and contain only letters or numbers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidPassword(String password) {
        return password.matches("^[A-Z][a-zA-Z0-9]*$");
    }

    private void saveToDatabase(String userId, String email) {
        User user = new User(email);
        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "User data saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
