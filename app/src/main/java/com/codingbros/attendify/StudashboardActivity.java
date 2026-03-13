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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class StudashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeName, tvAttendancePercentage, tvNoClassesMsg;
    private ProgressBar progressAttendance;
    private ImageView btnNotification, btnLogoutIcon;
    private BottomNavigationView bottomNavigationView;
    private GridLayout gridMenu;
    private androidx.cardview.widget.CardView cardAttendance;
    private RecyclerView recyclerUpcomingClasses;
    private StudTimetableAdapter timetableAdapter;
    private List<Map<String, String>> upcomingClassesList;
    private TextView btnSeeAll;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_devices");


        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        tvAttendancePercentage = findViewById(R.id.tv_attendance_percentage);
        progressAttendance = findViewById(R.id.progress_attendance);
        btnNotification = findViewById(R.id.btn_notification);
        btnLogoutIcon = findViewById(R.id.btn_logout_icon);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        gridMenu = findViewById(R.id.grid_menu);
        cardAttendance = findViewById(R.id.card_attendance);
        tvNoClassesMsg = findViewById(R.id.tv_no_classes_msg);
        btnSeeAll = findViewById(R.id.btn_see_all);

        recyclerUpcomingClasses = findViewById(R.id.recycler_upcoming_classes);
        recyclerUpcomingClasses.setLayoutManager(new LinearLayoutManager(this));
        upcomingClassesList = new ArrayList<>();
        timetableAdapter = new StudTimetableAdapter(upcomingClassesList);
        recyclerUpcomingClasses.setAdapter(timetableAdapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchStudentName(currentUser.getUid());
            calculateOverallAttendance(currentUser.getUid());
            // loadTodaysClasses() moved to onResume() for instant refresh
        } else {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
            return;
        }

        setupGridMenuListeners();

        if (cardAttendance != null) {
            cardAttendance.setOnClickListener(v -> startActivity(new Intent(this, StudAttendanceActivity.class)));
        }

        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> startActivity(new Intent(this, TodaysClassesActivity.class)));
        }

        btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        btnLogoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, AcademicCalendarActivity.class));
                return true;
            }
            if (itemId == R.id.nav_attendance) {
                startActivity(new Intent(this, StudAttendanceActivity.class));
                return true;
            }
            if (itemId == R.id.nav_marks) {
                startActivity(new Intent(this, StudMarksSubjectActivity.class));
                return true;
            }
            if (itemId == R.id.nav_timetable) {
                startActivity(new Intent(this, StudTimetableActivity.class));
                return true;
            }
            return false;
        });
    }

    // --- NEW: Added onResume to refresh data instantly ---
    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadTodaysClasses();
        }
    }

    private void setupGridMenuListeners() {
        if (gridMenu != null && gridMenu.getChildCount() == 4) {
            gridMenu.getChildAt(0).setOnClickListener(v -> startActivity(new Intent(this, NoticeBoardActivity.class)));
            gridMenu.getChildAt(1).setOnClickListener(v -> startActivity(new Intent(this, StudTimetableActivity.class)));
            gridMenu.getChildAt(2).setOnClickListener(v -> startActivity(new Intent(this, AcademicCalendarActivity.class)));
            gridMenu.getChildAt(3).setOnClickListener(v -> startActivity(new Intent(this, StudMarksSubjectActivity.class)));
        }
    }

    private void fetchStudentName(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (document.exists() && document.getString("name") != null) {
                tvWelcomeName.setText("Hi, " + document.getString("name").split(" ")[0] + "!");
            } else {
                tvWelcomeName.setText("Hi, Student!");
            }
        });
    }

    private void calculateOverallAttendance(String studUid) {
        db.collection("attendance").get().addOnSuccessListener(queryDocumentSnapshots -> {
            long totalClasses = 0, attendedClasses = 0;

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.contains("attendanceData")) {
                    Map<String, String> attData = (Map<String, String>) doc.get("attendanceData");
                    if (attData != null && attData.containsKey(studUid)) {
                        totalClasses++;
                        if ("Present".equalsIgnoreCase(attData.get(studUid))) {
                            attendedClasses++;
                        }
                    }
                }
            }

            int calcPercentage = totalClasses > 0 ? (int) ((attendedClasses * 100) / totalClasses) : 0;
            tvAttendancePercentage.setText(calcPercentage + "%");
            progressAttendance.setProgress(calcPercentage);

            if (calcPercentage < 75 && totalClasses > 0) {
                tvAttendancePercentage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvAttendancePercentage.setTextColor(android.graphics.Color.parseColor("#6FBCC7"));
            }
        });
    }

    private void showNoClassesMessage(String message, boolean isErrorOrHoliday) {
        recyclerUpcomingClasses.setVisibility(View.GONE);
        btnSeeAll.setVisibility(View.GONE);
        tvNoClassesMsg.setVisibility(View.VISIBLE);
        tvNoClassesMsg.setText(message);

        if (isErrorOrHoliday) {
            tvNoClassesMsg.setTextColor(android.graphics.Color.parseColor("#E53935")); // Red
        } else {
            tvNoClassesMsg.setTextColor(android.graphics.Color.parseColor("#757575")); // Gray
        }
    }

    private void loadTodaysClasses() {
        String todayDate = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        db.collection("holidays").document(todayDate).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String reason = doc.getString("reason");
                showNoClassesMessage("Classes cancelled today.\nHoliday: " + reason, true);
            } else {
                fetchClassesFromTimetable();
            }
        }).addOnFailureListener(e -> fetchClassesFromTimetable());
    }

    private void fetchClassesFromTimetable() {
        String currentDay = getDayString();

        if (currentDay.equals("Sunday") || currentDay.equals("Saturday")) {
            showNoClassesMessage("No classes on weekends!", false);
            return;
        }

        // --- FIXED: Now fetches from the global timetable collection ---
        db.collection("timetable").document(currentDay).get().addOnSuccessListener(documentSnapshot -> {
            upcomingClassesList.clear();

            if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                Map<String, Object> data = documentSnapshot.getData();

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, String> classDetails = (Map<String, String>) entry.getValue();

                        // --- NEW: Add the Break Emoji if it's a break ---
                        if ("true".equals(classDetails.get("is_break"))) {
                            String originalName = classDetails.get("subject_name");
                            classDetails.put("subject_name", "☕ " + originalName);
                        }

                        upcomingClassesList.add(classDetails);
                    }
                }
            }

            if (upcomingClassesList.isEmpty()) {
                showNoClassesMessage("No classes scheduled for today.", false);
            } else {
                java.util.Collections.sort(upcomingClassesList, (c1, c2) -> {
                    return Integer.compare(parseTimeToMinutes(c1.get("time_from")), parseTimeToMinutes(c2.get("time_from")));
                });

                // Display only the next upcoming class
                if (upcomingClassesList.size() > 1) {
                    Map<String, String> firstClass = upcomingClassesList.get(0);
                    upcomingClassesList.clear();
                    upcomingClassesList.add(firstClass);
                }

                tvNoClassesMsg.setVisibility(View.GONE);
                recyclerUpcomingClasses.setVisibility(View.VISIBLE);
                btnSeeAll.setVisibility(View.VISIBLE);
                timetableAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> showNoClassesMessage("Error loading classes.", true));
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

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0;
        try {
            timeStr = timeStr.trim().toUpperCase();
            boolean isPM = timeStr.contains("PM");
            int hours = Integer.parseInt(timeStr.replaceAll("[^0-9:]", "").split(":")[0]);
            int minutes = Integer.parseInt(timeStr.replaceAll("[^0-9:]", "").split(":")[1]);

            if (isPM && hours != 12) hours += 12;
            if (timeStr.contains("AM") && hours == 12) hours = 0;

            return (hours * 60) + minutes;
        } catch (Exception e) { return 0; }
    }
}