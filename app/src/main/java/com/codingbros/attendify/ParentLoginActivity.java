package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ParentLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvForgot;

    private FirebaseAuth mAuth;

    private static final String DOMAIN = "@student.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_signin);

        btnSignIn.setOnClickListener(v -> loginStudent());
    }

    private void loginStudent() {

        String enrollment = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(enrollment)) {
            etEmail.setError("Enter Enrollment Number");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter Password");
            return;
        }

        String email = enrollment + DOMAIN;

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter valid Enrollment Number", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        startActivity(new Intent(
                                ParentLoginActivity.this,
                                ParentDashboardActivity.class
                        ));
                        finish();

                    } else {
                        Toast.makeText(
                                this,
                                "Login failed. Wrong credentials",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
