package com.codingbros.attendify;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAttendanceDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvTotal, tvPresent, tvAbsent, tvMonthYear;
    private RecyclerView recyclerCalendar;
    private FirebaseFirestore db;
    private String subjectName, studentUid;

    // Key = Day of Month (e.g., "15"), Value = "Present" or "Absent"
    private Map<String, String> attendanceStatusMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance_detail);

        subjectName = getIntent().getStringExtra("subject_name");
        db = FirebaseFirestore.getInstance();
        studentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvTitle = findViewById(R.id.tv_subject_title);
        tvTotal = findViewById(R.id.tv_total_lectures);
        tvPresent = findViewById(R.id.tv_present_count);
        tvAbsent = findViewById(R.id.tv_absent_count);
        tvMonthYear = findViewById(R.id.tv_month_year);
        recyclerCalendar = findViewById(R.id.recycler_calendar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvTitle.setText(subjectName);

        // Setup Calendar Grid (7 columns for 7 days)
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        fetchAttendanceData();
    }

    private void fetchAttendanceData() {
        // Query Attendance Collection where 'subject' equals selected subject
        db.collection("attendance")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = 0;
                    int present = 0;
                    int absent = 0;

                    // Format to parse date from doc ID or field.
                    // Assuming date stored as "dd-MM-yyyy" in field 'date'
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH); // 0-indexed

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();

                        // Check if attendance data exists
                        if (data.containsKey("attendanceData")) {
                            Map<String, String> studentsMap = (Map<String, String>) data.get("attendanceData");

                            // Check if THIS student is in the map
                            if (studentsMap != null && studentsMap.containsKey(studentUid)) {
                                String status = studentsMap.get(studentUid);
                                String dateStr = doc.getString("date"); // "25-02-2026"

                                // Increment Stats
                                total++;
                                if ("Present".equals(status)) present++;
                                else if ("Absent".equals(status)) absent++;

                                // Map for Calendar (Only for current month)
                                if (dateStr != null) {
                                    try {
                                        cal.setTime(sdf.parse(dateStr));
                                        if (cal.get(Calendar.MONTH) == currentMonth) {
                                            String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
                                            attendanceStatusMap.put(day, status);
                                        }
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }
                        }
                    }

                    // Update UI Stats
                    tvTotal.setText(String.valueOf(total));
                    tvPresent.setText(String.valueOf(present));
                    tvAbsent.setText(String.valueOf(absent));

                    // Render Calendar
                    setupCalendarAdapter();

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records", Toast.LENGTH_SHORT).show());
    }

    private void setupCalendarAdapter() {
        // Generate days for current month
        List<String> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Update Header Text
        SimpleDateFormat monthDate = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthDate.format(cal.getTime()));

        cal.set(Calendar.DAY_OF_MONTH, 1);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // Sunday = 0

        // Add empty slots for days before 1st of month
        for (int i = 0; i < startDayOfWeek; i++) {
            days.add("");
        }

        // Add actual days
        for (int i = 1; i <= maxDays; i++) {
            days.add(String.valueOf(i));
        }

        CalendarAdapter adapter = new CalendarAdapter(days, attendanceStatusMap);
        recyclerCalendar.setAdapter(adapter);
    }
}