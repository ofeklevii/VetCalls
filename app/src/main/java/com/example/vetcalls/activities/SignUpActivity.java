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

/**
 * SignUpActivity handles the first step of the user registration process in the VetCalls application.
 * This activity is responsible for collecting and validating the user's email address before
 * proceeding to the password creation step.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Email address input and validation</li>
 *   <li>Comprehensive email format verification using regex patterns</li>
 *   <li>Navigation to password creation activity upon successful validation</li>
 *   <li>Return navigation to login screen</li>
 *   <li>User-friendly error messaging for invalid inputs</li>
 * </ul>
 *
 * <p>Registration Flow:</p>
 * <ol>
 *   <li>User enters email address</li>
 *   <li>System validates email format and structure</li>
 *   <li>Upon successful validation, user proceeds to SignUpPasswordActivity</li>
 *   <li>Email address is passed to the next activity for account creation</li>
 * </ol>
 *
 * <p>Validation Rules:</p>
 * The email validation implements multiple layers of verification including:
 * <ul>
 *   <li>Non-empty field validation</li>
 *   <li>Regex pattern matching for proper email structure</li>
 *   <li>Required presence of '@' and '.' characters</li>
 *   <li>Domain validation to ensure proper email format</li>
 * </ul>
 *
 * @author Ofek Levi
 * @version 1.0
 * @since 1.0
 * @see SignUpPasswordActivity
 * @see LoginActivity
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    /**
     * Initializes the sign-up activity and sets up the user interface components.
     * This method configures email input validation, button click listeners,
     * and navigation logic for the registration flow.
     *
     * <p>Setup Process:</p>
     * <ol>
     *   <li>Initialize UI components (email input, buttons)</li>
     *   <li>Configure next button for email validation and progression</li>
     *   <li>Configure back button for return navigation to login</li>
     *   <li>Set up error handling and user feedback mechanisms</li>
     * </ol>
     *
     * @param savedInstanceState If the activity is being re-initialized after being
     *                          previously shut down, this Bundle contains the most recent
     *                          data. Otherwise, it is null.
     */
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
                Intent intent = new Intent(SignUpActivity.this, SignUpPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please enter a valid email address (e.g. name@gmail.com)", Toast.LENGTH_SHORT).show();
            }
        });

        backToLoginButton.setOnClickListener(view -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Validates the provided email address using comprehensive format checking.
     * This method implements multiple validation layers to ensure the email address
     * meets proper formatting standards and contains required structural elements.
     *
     * <p>Validation Process:</p>
     * <ol>
     *   <li>Check for empty or null input</li>
     *   <li>Verify email format using regex pattern matching</li>
     *   <li>Confirm presence of required '@' symbol</li>
     *   <li>Confirm presence of domain separator '.'</li>
     * </ol>
     *
     * <p>The regex pattern ensures:</p>
     * <ul>
     *   <li>Valid characters in local part (letters, numbers, dots, underscores, hyphens)</li>
     *   <li>Proper domain structure with valid domain name characters</li>
     *   <li>Top-level domain with minimum 2 characters</li>
     * </ul>
     *
     * @param email The email address string to validate
     * @return true if the email address is valid and properly formatted, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email).matches() && email.contains("@") && email.contains(".");
    }
}
