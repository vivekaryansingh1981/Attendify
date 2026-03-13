package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private TextView tvHeaderName, tvAttendancePercent;
    private ProgressBar progressAttendance;
    private ImageView btnLogout, btnNotification;
    private BottomNavigationView bottomNavigationView;

    // Dashboard Cards
    private CardView cardAttendance, cardMarks, cardNotice, cardCalendar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // 1. Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_devices");

        // 2. Link Header Views
        tvHeaderName = findViewById(R.id.tv_header_name);
        tvAttendancePercent = findViewById(R.id.tv_attendance_percentage);
        progressAttendance = findViewById(R.id.progress_attendance);
        btnLogout = findViewById(R.id.btn_logout_icon);
        btnNotification = findViewById(R.id.btn_notification);

        // 3. Link Dashboard Action Cards
        cardAttendance = findViewById(R.id.card_action_attendance);
        cardMarks = findViewById(R.id.card_action_marks);
        cardNotice = findViewById(R.id.card_action_notice);
        cardCalendar = findViewById(R.id.card_action_calendar);

        // 4. Link Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_nav_parent);

        // 5. Fetch Data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String studentUid = currentUser.getUid();
            fetchStudentName(studentUid);
            calculateOverallAttendance(studentUid); // NEW: Calculate Real Attendance
        }

        // 6. Logout Logic
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ParentDashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 7. Top Notification (Bell) Icon Logic - STRICTLY FOR LECTURE UPDATES/ALERTS
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // 8. DASHBOARD CARD CLICKS
        cardAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, StudentAttendanceActivity.class);
            startActivity(intent);
        });

        cardMarks.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, StudentMarksSubjectActivity.class);
            startActivity(intent);
        });

        // Notice Board Card Click
        cardNotice.setOnClickListener(v -> openNoticeBoard());

        // Academic Calendar Card Click
        cardCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, AcademicCalendarActivity.class);
            startActivity(intent);
        });

        // 9. Navigation Logic (Synced with Cards)
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_attendance) {
                    cardAttendance.performClick();
                    return true;
                } else if (id == R.id.nav_marks) {
                    cardMarks.performClick();
                    return true;
                } else if (id == R.id.nav_notice) {
                    openNoticeBoard(); // Notice Board from Bottom Nav
                    return true;
                } else if (id == R.id.nav_calendar) {
                    cardCalendar.performClick();
                    return true;
                }
                return false;
            });
        }
    }

    // --- HELPER METHOD: Open Notice Board ---
    private void openNoticeBoard() {
        Intent intent = new Intent(ParentDashboardActivity.this, NoticeBoardActivity.class);
        startActivity(intent);
    }

    // --- UPDATED: Only fetches the name from the user profile now ---
    private void fetchStudentName(String studentUid) {
        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fullName = document.getString("name");
                        if (fullName != null && !fullName.isEmpty()) {
                            String firstName = fullName.split(" ")[0];
                            tvHeaderName.setText(firstName + "'s Academics");
                        } else {
                            tvHeaderName.setText("Student's Academics");
                        }
                    } else {
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // --- NEW: Calculates REAL Overall Attendance directly from the Faculty's records ---
    private void calculateOverallAttendance(String studentUid) {
        db.collection("attendance").get().addOnSuccessListener(queryDocumentSnapshots -> {
            long totalClasses = 0;
            long attendedClasses = 0;

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.contains("attendanceData")) {
                    Map<String, String> attData = (Map<String, String>) doc.get("attendanceData");

                    // Check if this student is part of this specific class record
                    if (attData != null && attData.containsKey(studentUid)) {
                        totalClasses++; // Found a class the student is enrolled in

                        String status = attData.get(studentUid);
                        if ("Present".equalsIgnoreCase(status)) {
                            attendedClasses++; // Student was present
                        }
                    }
                }
            }

            int calculatedPercentage = 0;
            if (totalClasses > 0) {
                calculatedPercentage = (int) ((attendedClasses * 100) / totalClasses);
            }

            // Update UI
            progressAttendance.setProgress(calculatedPercentage);
            tvAttendancePercent.setText(calculatedPercentage + "%");

            if (calculatedPercentage < 75 && totalClasses > 0) {
                tvAttendancePercent.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvAttendancePercent.setTextColor(android.graphics.Color.parseColor("#0C7779")); // Standard parent teal
            }

        }).addOnFailureListener(e -> {
            tvAttendancePercent.setText("--%");
            Toast.makeText(this, "Error calculating attendance.", Toast.LENGTH_SHORT).show();
        });
    }
}