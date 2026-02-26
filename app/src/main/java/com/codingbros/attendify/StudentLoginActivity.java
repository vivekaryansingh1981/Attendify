package com.codingbros.attendify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class StudentLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;
    private FirebaseAuth mAuth;
    private static final String DOMAIN = "@student.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

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

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // SAVE SESSION
                        SharedPreferences prefs = getSharedPreferences("AttendifyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("userRole", "student");
                        editor.apply();

                        startActivity(new Intent(StudentLoginActivity.this, StudashboardActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Login failed. Wrong credentials", Toast.LENGTH_LONG).show();
                    }
                });
    }
}