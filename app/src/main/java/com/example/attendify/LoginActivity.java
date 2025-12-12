package com.example.attendify;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText etEmail, etPassword;
    Button btnSignIn;
    TextView tvRegister, tvForgot;
    ImageButton btnGoogle, btnFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Linking XML components
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_signin);
        tvRegister = findViewById(R.id.tv_register);
        tvForgot = findViewById(R.id.tv_forgot);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);

        // Login button click
        btnSignIn.setOnClickListener(v -> validateLogin());

        // Register screen open
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Forgot password screen (optional)
        tvForgot.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        );

        // Google button
        btnGoogle.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Google Login Clicked", Toast.LENGTH_SHORT).show()
        );

        // Facebook button
        btnFacebook.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Facebook Login Clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private void validateLogin() {
        String email = etEmail.getText().toString();
        String pass = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("Enter password");
            return;
        }

        // Success message for now
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

        // After login → open HomeActivity
        Intent intent = new Intent(LoginActivity.this, StudashboardActivity.class);
        startActivity(intent);
    }
}
