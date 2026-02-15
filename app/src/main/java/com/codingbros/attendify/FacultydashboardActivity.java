package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class FacultydashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private ImageView btnLogoutIcon, btnNotification;
    private BottomNavigationView bottomNavigationView;

    // NEW: Container for the dynamic class list
    private LinearLayout containerTodaysClasses;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facultydashboard);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Link UI Components
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        btnLogoutIcon = findViewById(R.id.btn_logout_icon);
        btnNotification = findViewById(R.id.btn_notification);
        bottomNavigationView = findViewById(R.id.bottom_nav_faculty);

        // NEW: Link the container
        containerTodaysClasses = findViewById(R.id.container_todays_classes);

        // 3. Fetch Data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Fetch Name
            fetchFacultyData(uid);

            // NEW: Fetch Today's Schedule
            loadTodaysClasses(uid);
        }

        // 4. Logout Logic
        btnLogoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(FacultydashboardActivity.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FacultydashboardActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 5. Notification Logic
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(FacultydashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // 6. Bottom Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_attendance) {
                startActivity(new Intent(this, MarkAttendanceActivity.class));
                Toast.makeText(this, "Mark Attendance Screen", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_timetable) {
                startActivity(new Intent(this, EditTimetableActivity.class));
                Toast.makeText(this, "Edit Time Table Screen", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_marks) {
                startActivity(new Intent(this, EnterMarksActivity.class));
                Toast.makeText(this, "Enter Marks Screen", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_status) {
                startActivity(new Intent(this, StatusActivity.class));
                Toast.makeText(this, "Status Screen", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Optional: Link Quick Action Grid Buttons (if you want them to work)
        findViewById(R.id.btn_mark_attendance).setOnClickListener(v ->
                Toast.makeText(this, "Mark Attendance", Toast.LENGTH_SHORT).show()
        );
    }

    // --- DATA FETCHING METHODS ---

    private void fetchFacultyData(String userId) {
        DocumentReference docRef = db.collection("faculty").document(userId);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("name");
                    if (name != null) {
                        tvWelcomeName.setText("Hi, Prof. " + name);
                    }
                } else {
                    Toast.makeText(this, "Faculty Profile not found.", Toast.LENGTH_SHORT).show();
                    tvWelcomeName.setText("Hi, Faculty");
                }
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- NEW: Load Real-Time Classes ---
    private void loadTodaysClasses(String uid) {
        String currentDay = getDayString();

        // Handle Weekends
        if (currentDay.equals("Sunday") || currentDay.equals("Saturday")) {
            showNoClassesMessage("No classes on weekends!");
            return;
        }

        db.collection("faculty").document(uid)
                .collection("timetable").document(currentDay)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    containerTodaysClasses.removeAllViews(); // Clear "Loading..." or old views

                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();

                        // Use TreeMap to sort keys automatically (slot_0, slot_1, etc.)
                        Map<String, Object> sortedData = new TreeMap<>(data);
                        boolean hasClasses = false;

                        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                            // Check if this slot actually contains class data
                            if (entry.getValue() instanceof Map) {
                                Map<String, String> classDetails = (Map<String, String>) entry.getValue();
                                addClassView(classDetails);
                                hasClasses = true;
                            }
                        }

                        if (!hasClasses) {
                            showNoClassesMessage("No classes scheduled for today.");
                        }
                    } else {
                        showNoClassesMessage("No timetable found for " + currentDay);
                    }
                })
                .addOnFailureListener(e ->
                        showNoClassesMessage("Error loading classes.")
                );
    }

    private void addClassView(Map<String, String> details) {
        // 1. Inflate the single item layout
        View view = LayoutInflater.from(this).inflate(R.layout.item_todays_class, containerTodaysClasses, false);

        // 2. Find views inside that layout
        TextView tvTime = view.findViewById(R.id.tv_class_time);
        TextView tvSubject = view.findViewById(R.id.tv_class_subject);
        TextView tvTeacher = view.findViewById(R.id.tv_teacher_name); // Optional

        // 3. Extract Data
        String subject = details.get("subject_name");
        String timeFrom = details.get("time_from");
        String timeTo = details.get("time_to");

        // 4. Set Text
        tvTime.setText(timeFrom + " - " + timeTo);
        tvSubject.setText(subject);

        // 5. Add to the main list container
        containerTodaysClasses.addView(view);
    }

    private void showNoClassesMessage(String message) {
        containerTodaysClasses.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tv.setTextSize(16);
        tv.setPadding(10, 20, 10, 20);
        containerTodaysClasses.addView(tv);
    }

    private String getDayString() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        switch (day) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "Monday";
        }
    }
}