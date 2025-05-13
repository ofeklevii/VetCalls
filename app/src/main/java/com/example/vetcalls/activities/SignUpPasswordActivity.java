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

public class SignUpPasswordActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private Switch vetSwitch;
    private FirebaseAuth firebaseAuth;

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
                                    // שימוש בפונקציה מ-FirestoreUserHelper
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

        // טיפול בלחיצה על כפתור החזרה
        backButton.setOnClickListener(v -> {
            // חזרה למסך הקודם (Sign Up)
            onBackPressed(); // זה יחזיר אותך למסך הקודם בערימת הפעילויות
            // לחלופין, אפשר להשתמש גם ב:
            // finish();
        });
    }

    private boolean isValidPassword(String password) {
        // בדיקה שהסיסמה מתחילה באות גדולה, מכילה רק אותיות ומספרים, ובאורך של לפחות 6 תווים
        return password.matches("^[A-Z][a-zA-Z0-9]{5,}$");
    }
}