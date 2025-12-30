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
    private TextView tvWelcomeName, tvAttendancePercentage, tvUpcomingClass;
    private ProgressBar progressAttendance;
    private ImageView btnNotification;
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
        bottomNavigationView = findViewById(R.id.bottom_nav);

        // (Optional) Link other grid items if you need them clickable later
        // View attendanceTrackerBtn = findViewById(R.id.btn_tracker);

        // 3. Check User Session
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchStudentData(currentUser.getUid());
        } else {
            // If not logged in, send back to login screen
            sendToLogin();
        }

        // 4. Temporary Logout Listener (Clicking the notification bell logs you out for now)
        // You can move this to the "Profile" tab in the BottomNavigation later.
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(StudashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // 5. Setup Bottom Navigation (Placeholder logic)
        // bottomNavigationView.setOnItemSelectedListener(item -> {
        //    switch (item.getItemId()) {
        //        case R.id.nav_home: return true;
        //        case R.id.nav_profile: return true;
        //    }
        //    return false;
        // });
    }

    private void fetchStudentData(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    // 1. Get Name
                    String name = document.getString("name");
                    if (name != null) {
                        String firstName = name.split(" ")[0];
                        tvWelcomeName.setText("Hi, " + firstName + "!");
                    }

                    // 2. Get the Two Numbers (Total vs Attended)
                    long totalClasses = 0;
                    long attendedClasses = 0;

                    if (document.contains("total_classes")) {
                        totalClasses = document.getLong("total_classes");
                    }
                    if (document.contains("attended_classes")) {
                        attendedClasses = document.getLong("attended_classes");
                    }

                    // 3. Calculate Percentage Logic
                    int calculatedPercentage = 0;

                    if (totalClasses > 0) {
                        // Formula: (Attended / Total) * 100
                        // We multiply by 100 first to avoid decimal issues in integer division
                        calculatedPercentage = (int) ((attendedClasses * 100) / totalClasses);
                    }

                    // 4. Update UI
                    tvAttendancePercentage.setText(calculatedPercentage + "%");
                    progressAttendance.setProgress(calculatedPercentage);

                    // Change color if attendance is low (e.g., below 75%)
                    if(calculatedPercentage < 75) {
                        tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.black));
                    }

                } else {
                    Toast.makeText(this, "Student data not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendToLogin() {
        Intent intent = new Intent(this, StudentLoginActivity.class);
        startActivity(intent);
        finish();
    }
}