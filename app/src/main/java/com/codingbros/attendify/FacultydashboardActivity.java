package com.codingbros.attendify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.os.Build;

public class FacultydashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private ImageView btnLogoutIcon, btnNotification;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout containerTodaysClasses;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Set<String> myRegisteredSubjects = new HashSet<>();

    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAuth.getCurrentUser() != null) {
                fetchRegisteredSubjectsAndLoadClasses();
            }
            refreshHandler.postDelayed(this, 60000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facultydashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_devices");

        // Check for Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please allow Attendify to set exact alarms for class reminders.", Toast.LENGTH_LONG).show();
            }
        }

        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        btnLogoutIcon = findViewById(R.id.btn_logout_icon);
        btnNotification = findViewById(R.id.btn_notification);
        bottomNavigationView = findViewById(R.id.bottom_nav_faculty);
        containerTodaysClasses = findViewById(R.id.container_todays_classes);

        findViewById(R.id.btn_mark_attendance).setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class)));
        findViewById(R.id.btn_enter_marks).setOnClickListener(v -> startActivity(new Intent(this, EnterMarksActivity.class)));
        findViewById(R.id.btn_edit_timetable).setOnClickListener(v -> startActivity(new Intent(this, EditTimetableActivity.class)));
        findViewById(R.id.btn_status).setOnClickListener(v -> startActivity(new Intent(this, FacultyOtherFeaturesActivity.class)));

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchFacultyData(currentUser.getUid());
        }

        btnLogoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            if (itemId == R.id.nav_attendance) {
                startActivity(new Intent(this, MarkAttendanceActivity.class));
                return true;
            }
            if (itemId == R.id.nav_timetable) {
                startActivity(new Intent(this, EditTimetableActivity.class));
                return true;
            }
            if (itemId == R.id.nav_marks) {
                startActivity(new Intent(this, EnterMarksActivity.class));
                return true;
            }
            if (itemId == R.id.nav_others) {
                startActivity(new Intent(this, FacultyOtherFeaturesActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            fetchRegisteredSubjectsAndLoadClasses();
        }
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void fetchFacultyData(String userId) {
        db.collection("faculty").document(userId).get().addOnSuccessListener(document -> {
            if (document.exists() && document.getString("name") != null) {
                tvWelcomeName.setText("Hello,\nProf. " + document.getString("name"));
            } else {
                tvWelcomeName.setText("Hello, Faculty");
            }
        });
    }

    private void fetchRegisteredSubjectsAndLoadClasses() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("faculty").document(uid).collection("subjects").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myRegisteredSubjects.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            myRegisteredSubjects.add(name);
                        }
                    }
                    loadTodaysClasses();
                })
                .addOnFailureListener(e -> loadTodaysClasses());
    }

    private void loadTodaysClasses() {
        String todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

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

        db.collection("timetable").document(currentDay)
                .get().addOnSuccessListener(documentSnapshot -> {
                    containerTodaysClasses.removeAllViews();

                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Map<String, Object> sortedData = new TreeMap<>(data);

                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        Calendar nowCal = Calendar.getInstance();
                        Map<String, String> classToDisplay = null;

                        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                Map<String, String> details = (Map<String, String>) entry.getValue();

                                scheduleClassNotification(details.get("subject_name"), details.get("time_from"));

                                if (classToDisplay == null) {
                                    try {
                                        Date timeToDate = sdf.parse(details.get("time_to"));
                                        Calendar toCal = Calendar.getInstance();
                                        toCal.setTime(timeToDate);

                                        Calendar currentCompare = Calendar.getInstance();
                                        currentCompare.set(Calendar.HOUR_OF_DAY, toCal.get(Calendar.HOUR_OF_DAY));
                                        currentCompare.set(Calendar.MINUTE, toCal.get(Calendar.MINUTE));
                                        currentCompare.set(Calendar.SECOND, 0);

                                        if (nowCal.before(currentCompare)) {
                                            classToDisplay = details;
                                        }
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }
                        }

                        if (classToDisplay != null) {
                            addClassView(classToDisplay);
                        } else {
                            showNoClassesMessage("All classes for today are completed.", false);
                        }
                    } else {
                        showNoClassesMessage("No timetable found for " + currentDay, false);
                    }
                }).addOnFailureListener(e -> showNoClassesMessage("Error loading classes.", true));
    }

    private void scheduleClassNotification(String subjectName, String timeFrom) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date timeFromDate = sdf.parse(timeFrom);

            Calendar alarmCal = Calendar.getInstance();
            Calendar parsedTime = Calendar.getInstance();
            parsedTime.setTime(timeFromDate);

            alarmCal.set(Calendar.HOUR_OF_DAY, parsedTime.get(Calendar.HOUR_OF_DAY));
            alarmCal.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
            alarmCal.set(Calendar.SECOND, 0);

            alarmCal.add(Calendar.MINUTE, -5);

            if (alarmCal.getTimeInMillis() > System.currentTimeMillis()) {
                Intent intent = new Intent(this, ClassAlertReceiver.class);
                intent.putExtra("subject_name", subjectName);
                intent.putExtra("time_from", timeFrom);

                int requestCode = subjectName.hashCode() + timeFrom.hashCode();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addClassView(Map<String, String> details) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_todays_class, containerTodaysClasses, false);

        TextView tvTime = view.findViewById(R.id.tv_class_time);
        TextView tvSubject = view.findViewById(R.id.tv_class_subject);
        TextView btnMarkNow = view.findViewById(R.id.btn_mark_now);

        tvTime.setText(details.get("time_from") + " - " + details.get("time_to"));

        if ("true".equals(details.get("is_break"))) {
            tvSubject.setText("☕ " + details.get("subject_name"));
            tvSubject.setTextColor(android.graphics.Color.parseColor("#D84315"));
            btnMarkNow.setVisibility(View.GONE);
        } else {
            String subjectName = details.get("subject_name");
            tvSubject.setText(subjectName);
            btnMarkNow.setVisibility(View.VISIBLE);

            btnMarkNow.setOnClickListener(v -> {
                if (myRegisteredSubjects.contains(subjectName)) {
                    Intent intent = new Intent(FacultydashboardActivity.this, StudentListActivity.class);
                    intent.putExtra("subject_name", subjectName);
                    intent.putExtra("subject_abbr", details.get("subject_abbr"));
                    startActivity(intent);
                } else {
                    Toast.makeText(FacultydashboardActivity.this,
                            "Access Denied: Please add '" + subjectName + "' in the Mark Attendance section first.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        containerTodaysClasses.addView(view);
    }

    private void showNoClassesMessage(String message, boolean isHoliday) {
        containerTodaysClasses.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(16);
        tv.setPadding(10, 20, 10, 20);

        if (isHoliday) {
            tv.setTextColor(android.graphics.Color.parseColor("#E53935"));
        } else {
            tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
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