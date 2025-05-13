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

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    // ביטוי רגולרי לבדיקת פורמט אימייל תקין (כולל דומיין)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailEditText = findViewById(R.id.emailInput);
        Button nextButton = findViewById(R.id.nextButton);
        Button backToLoginButton = findViewById(R.id.backToLoginButton);

        nextButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();

            if (isValidEmail(email)) {
                // Pass the email to the next activity
                Intent intent = new Intent(SignUpActivity.this, SignUpPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            } else {
                // הודעת שגיאה מפורטת יותר
                Toast.makeText(this, "Please enter a valid email address (e.g. name@gmail.com)", Toast.LENGTH_SHORT).show();
            }
        });

        // טיפול בלחיצה על כפתור החזרה למסך הלוגין
        backToLoginButton.setOnClickListener(view -> {
            // מעבר למסך הלוגין
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // סגירת האקטיביטי הנוכחית
        });
    }

    private boolean isValidEmail(String email) {
        // בדיקה שהשדה לא ריק והאימייל תואם את התבנית
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        // בדיקה מורכבת יותר באמצעות ביטוי רגולרי
        return EMAIL_PATTERN.matcher(email).matches() && email.contains("@") && email.contains(".");
    }
}