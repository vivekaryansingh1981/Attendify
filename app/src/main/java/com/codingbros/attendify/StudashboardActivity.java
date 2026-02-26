package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private GridLayout gridMenu;

    // The CardView for overall attendance
    private androidx.cardview.widget.CardView cardAttendance;

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
        btnLogoutIcon = findViewById(R.id.btn_logout_icon);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        gridMenu = findViewById(R.id.grid_menu);

        // Link the CardView
        cardAttendance = findViewById(R.id.card_attendance);

        // 3. Check User Session
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchStudentData(currentUser.getUid());
        } else {
            sendToLogin();
            return;
        }

        // 4. Setup Grid Menu & Card Click Listeners
        setupGridMenuListeners();

        // Open Attendance Tracker when clicking the big percentage card
        if (cardAttendance != null) {
            cardAttendance.setOnClickListener(v -> {
                startActivity(new Intent(this, StudAttendanceActivity.class));
            });
        }

        // 5. Setup Header Icons
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        btnLogoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 6. Setup Bottom Navigation Routing
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, AcademicCalendarActivity.class));
                return true;
            } else if (itemId == R.id.nav_attendance) {
                startActivity(new Intent(this, StudAttendanceActivity.class));
                return true;
            } else if (itemId == R.id.nav_marks) {
                startActivity(new Intent(this, StudMarksSubjectActivity.class));
                return true;
            } else if (itemId == R.id.nav_timetable) {
                // UPDATED: Now routes to StudTimetableActivity
                startActivity(new Intent(this, StudTimetableActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupGridMenuListeners() {
        // Since the XML items don't have IDs, we fetch them by their index in the GridLayout
        if (gridMenu != null && gridMenu.getChildCount() == 4) {

            // 0: Notice Board
            LinearLayout btnNotice = (LinearLayout) gridMenu.getChildAt(0);
            btnNotice.setOnClickListener(v ->
                    startActivity(new Intent(this, NoticeBoardActivity.class))
            );

            // 1: Timetable (UPDATED: Now routes to StudTimetableActivity)
            LinearLayout btnTimetable = (LinearLayout) gridMenu.getChildAt(1);
            btnTimetable.setOnClickListener(v ->
                    startActivity(new Intent(this, StudTimetableActivity.class))
            );

            // 2: Calendar
            LinearLayout btnCalendar = (LinearLayout) gridMenu.getChildAt(2);
            btnCalendar.setOnClickListener(v ->
                    startActivity(new Intent(this, AcademicCalendarActivity.class))
            );

            // 3: Unit Test Marks
            LinearLayout btnMarks = (LinearLayout) gridMenu.getChildAt(3);
            btnMarks.setOnClickListener(v ->
                    startActivity(new Intent(this, StudMarksSubjectActivity.class))
            );
        }
    }

    private void fetchStudentData(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();

                if (document.exists()) {
                    // --- Get Name ---
                    String name = document.getString("name");
                    if (name != null && !name.isEmpty()) {
                        String firstName = name.split(" ")[0];
                        tvWelcomeName.setText("Hi, " + firstName + "!");
                    }

                    // --- Calculate Attendance Percentage ---
                    long totalClasses = 0;
                    long attendedClasses = 0;

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

                    int calculatedPercentage = 0;
                    if (totalClasses > 0) {
                        calculatedPercentage = (int) ((attendedClasses * 100) / totalClasses);
                    }

                    // --- Update UI ---
                    tvAttendancePercentage.setText(calculatedPercentage + "%");
                    progressAttendance.setProgress(calculatedPercentage);

                    if(calculatedPercentage < 75) {
                        tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.black));
                    }

                } else {
                    tvWelcomeName.setText("Hi, Student!");
                    tvAttendancePercentage.setText("--%");
                }
            } else {
                Toast.makeText(this, "Error fetching profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendToLogin() {
        Intent intent = new Intent(this, StudentLoginActivity.class); // Standardized to StudLoginActivity
        startActivity(intent);
        finish();
    }
}