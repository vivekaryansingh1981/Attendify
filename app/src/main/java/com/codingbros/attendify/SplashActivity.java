package com.codingbros.attendify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("AttendifyPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            String role = prefs.getString("userRole", "");

            // If user is logged in via Firebase and our session exists, redirect them
            if (isLoggedIn && FirebaseAuth.getInstance().getCurrentUser() != null) {
                Intent intent;
                switch (role) {
                    case "faculty":
                        intent = new Intent(SplashActivity.this, FacultydashboardActivity.class);
                        break;
                    case "parent":
                        intent = new Intent(SplashActivity.this, ParentDashboardActivity.class);
                        break;
                    case "student":
                        intent = new Intent(SplashActivity.this, StudashboardActivity.class);
                        break;
                    default:
                        intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
                        break;
                }
                startActivity(intent);
            } else {
                // No session found, go to role selection
                startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
            }
            finish();
        }, SPLASH_DELAY);
    }
}