package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class FacultyLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_login);

        mAuth = FirebaseAuth.getInstance();

        // 🔴 FORCE LOGOUT (VERY IMPORTANT)
        mAuth.signOut();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_signin);

        btnSignIn.setOnClickListener(v -> loginFaculty());
    }

    private void loginFaculty() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter Email Address");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter Password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        startActivity(new Intent(
                                FacultyLoginActivity.this,
                                FacultydashboardActivity.class
                        ));
                        finish();

                    } else {

                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(this, "Login failed. Wrong credentials", Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(this, "Faculty account not found", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
