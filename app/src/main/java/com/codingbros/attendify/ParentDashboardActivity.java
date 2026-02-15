package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ParentDashboardActivity extends AppCompatActivity {

    private TextView tvHeaderName, tvAttendancePercent;
    private ProgressBar progressAttendance;
    private ImageView btnLogout, btnNotification;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // 1. Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Link Views
        tvHeaderName = findViewById(R.id.tv_header_name);
        tvAttendancePercent = findViewById(R.id.tv_attendance_percentage);
        progressAttendance = findViewById(R.id.progress_attendance);
        btnLogout = findViewById(R.id.btn_logout_icon);
        btnNotification = findViewById(R.id.btn_notification);

        // Link Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_nav_parent);

        // 3. Fetch Data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchStudentDetails(currentUser.getUid());
        }

        // 4. Logout Logic
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ParentDashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 5. Notification Logic
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // 6. Navigation Logic
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // We are already on Home, do nothing or refresh
                    return true;
                }
                else if (id == R.id.nav_calendar) {
                    // Navigate to Calendar Page (Placeholder Toast for now)
                    Toast.makeText(ParentDashboardActivity.this, "Calendar Clicked", Toast.LENGTH_SHORT).show();
                    // startActivity(new Intent(this, CalendarActivity.class));
                    return true;
                }
                else if (id == R.id.nav_attendance) {
                    // Navigate to Attendance Detail Page
                    Toast.makeText(ParentDashboardActivity.this, "Attendance Clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                else if (id == R.id.nav_marks) {
                    // Navigate to Marks Page
                    Toast.makeText(ParentDashboardActivity.this, "Marks Clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                else if (id == R.id.nav_timetable) {
                    // Navigate to Timetable Page
                    Toast.makeText(ParentDashboardActivity.this, "Timetable Clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }
    }

    private void fetchStudentDetails(String studentUid) {
        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // 1. NAME LOGIC (Get First Name)
                        String fullName = document.getString("name");
                        if (fullName != null && !fullName.isEmpty()) {
                            String firstName = fullName.split(" ")[0];
                            tvHeaderName.setText(firstName + "'s Academics");
                        } else {
                            tvHeaderName.setText("Student's Academics");
                        }

                        // --- 2. CALCULATE ATTENDANCE PERCENTAGE (FIXED) ---
                        long totalClasses = 0;
                        long attendedClasses = 0;

                        // Fetch raw numbers (handle Long or String types safely)
                        if (document.contains("total_classes")) {
                            Object totalObj = document.get("total_classes");
                            if (totalObj instanceof Number) totalClasses = ((Number) totalObj).longValue();
                            else if (totalObj instanceof String) totalClasses = Long.parseLong((String) totalObj);
                        }

                        if (document.contains("attended_classes")) {
                            Object attendedObj = document.get("attended_classes");
                            if (attendedObj instanceof Number) attendedClasses = ((Number) attendedObj).longValue();
                            else if (attendedObj instanceof String) attendedClasses = Long.parseLong((String) attendedObj);
                        }

                        // Calculate Logic
                        int calculatedPercentage = 0;
                        if (totalClasses > 0) {
                            // Formula: (Attended / Total) * 100
                            calculatedPercentage = (int) ((attendedClasses * 100) / totalClasses);
                        }

                        // Update UI
                        progressAttendance.setProgress(calculatedPercentage);
                        tvAttendancePercent.setText(calculatedPercentage + "%");

                    } else {
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}