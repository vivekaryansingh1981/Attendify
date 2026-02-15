package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudashboardActivity extends AppCompatActivity {

    // UI Components
    private TextView tvWelcomeName, tvAttendancePercentage;
    private ProgressBar progressAttendance;
    private ImageView btnNotification, btnLogoutIcon;
    private BottomNavigationView bottomNavigationView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studashboard);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Link Views to XML IDs
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        tvAttendancePercentage = findViewById(R.id.tv_attendance_percentage);
        progressAttendance = findViewById(R.id.progress_attendance);
        btnNotification = findViewById(R.id.btn_notification);
        btnLogoutIcon = findViewById(R.id.btn_logout_icon); // The new Logout Icon
        bottomNavigationView = findViewById(R.id.bottom_nav);

        // 3. Check User Session
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchStudentData(currentUser.getUid());
        } else {
            // If not logged in, send back to login screen
            sendToLogin();
        }

        // 4. NOTIFICATION CLICK LISTENER
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(StudashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // 5. LOGOUT CLICK LISTENER (Direct Logout)
        btnLogoutIcon.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();

            Toast.makeText(StudashboardActivity.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();

            // Return to Role Selection Screen and clear history
            Intent intent = new Intent(StudashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 6. BOTTOM NAVIGATION (Placeholder)
        // You can add logic here later to switch between Fragments
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // We are already on Home, do nothing or refresh
                return true;
            }
            else if (itemId == R.id.nav_calendar) {
                // Navigate to Calendar Page (Placeholder Toast for now)
                Toast.makeText(StudashboardActivity.this, "Calendar Clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(this, CalendarActivity.class));
                return true;
            }
            else if (itemId == R.id.nav_attendance) {
                // Navigate to Attendance Detail Page
                Toast.makeText(StudashboardActivity.this, "Attendance Clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            else if (itemId == R.id.nav_marks) {
                // Navigate to Marks Page
                Toast.makeText(StudashboardActivity.this, "Marks Clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            else if (itemId == R.id.nav_timetable) {
                // Navigate to Timetable Page
                Toast.makeText(StudashboardActivity.this, "Timetable Clicked", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void fetchStudentData(String userId) {
        // Access the 'users' collection for this student
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    // --- 1. Get Name ---
                    String name = document.getString("name");
                    if (name != null) {
                        // Takes the first name only (e.g., "Hi, Aman!")
                        String firstName = name.split(" ")[0];
                        tvWelcomeName.setText("Hi, " + firstName + "!");
                    }

                    // --- 2. Calculate Attendance Percentage ---
                    long totalClasses = 0;
                    long attendedClasses = 0;

                    // Fetch the raw numbers from Firebase
                    if (document.contains("total_classes")) {
                        totalClasses = document.getLong("total_classes");
                    }
                    if (document.contains("attended_classes")) {
                        attendedClasses = document.getLong("attended_classes");
                    }

                    // Calculate logic
                    int calculatedPercentage = 0;
                    if (totalClasses > 0) {
                        // Formula: (Attended / Total) * 100
                        calculatedPercentage = (int) ((attendedClasses * 100) / totalClasses);
                    }

                    // --- 3. Update UI ---
                    tvAttendancePercentage.setText(calculatedPercentage + "%");
                    progressAttendance.setProgress(calculatedPercentage);

                    // Optional: Change text color to Red if attendance is low
                    if(calculatedPercentage < 75) {
                        tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        // Reset to black (or your default color) if it's good
                        tvAttendancePercentage.setTextColor(getResources().getColor(R.color.black));
                    }

                } else {
                    // Document doesn't exist (Admin hasn't added data yet)
                    tvWelcomeName.setText("Hi, Student!");
                    tvAttendancePercentage.setText("--%");
                    Toast.makeText(this, "Profile not found. Contact Admin.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendToLogin() {
        Intent intent = new Intent(this, StudentLoginActivity.class);
        startActivity(intent);
        finish();
    }
}