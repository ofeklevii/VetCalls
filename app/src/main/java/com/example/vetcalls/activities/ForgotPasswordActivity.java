package com.example.vetcalls.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vetcalls.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput, newPasswordInput;
    private Button submitButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        submitButton = findViewById(R.id.submitButton);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        submitButton.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        String email = emailInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInput.setError("Password is required");
            newPasswordInput.requestFocus();
            return;
        }
        if (!newPassword.matches("^[A-Z][a-zA-Z0-9]*$")) {
            newPasswordInput.setError("Password must start with a capital letter and contain only letters or numbers");
            newPasswordInput.requestFocus();
            return;
        }

        // Check if email is registered and update password
        databaseReference.orderByChild("email").equalTo(email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot userSnapshot : task.getResult().getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        databaseReference.child(userId).child("password").setValue(newPassword).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ForgotPasswordActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Email not registered", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
