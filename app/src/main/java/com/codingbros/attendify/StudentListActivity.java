package com.codingbros.attendify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;
    private List<Map<String, String>> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private Button btnSubmit;
    private TextView tvSubjectTitle, tvDate;

    private String subjectName;
    private String todayDate;
    private boolean isLocked = false;
    private String docId; // Document ID: "Date_SubjectName"

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        // Get Intent Data
        subjectName = getIntent().getStringExtra("subject_name");

        // --- NEW: Get the abbreviation ---
        String subjectAbbr = getIntent().getStringExtra("subject_abbr");

        // Fallback just in case abbreviation is missing
        if (subjectAbbr == null || subjectAbbr.trim().isEmpty()) {
            subjectAbbr = subjectName;
        }

        todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // We keep subjectName here so the database path doesn't break
        docId = todayDate + "_" + subjectName;

        db = FirebaseFirestore.getInstance();

        // Init Views
        recyclerView = findViewById(R.id.recycler_students);
        btnSubmit = findViewById(R.id.btn_submit_attendance);
        tvSubjectTitle = findViewById(R.id.tv_subject_title);
        tvDate = findViewById(R.id.tv_current_date);
        ImageView btnBack = findViewById(R.id.btn_back);

        // --- CHANGED: Set the title to the abbreviation ---
        tvSubjectTitle.setText(subjectAbbr);
        tvDate.setText("Date: " + todayDate);

        // Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(studentList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadStudents();

        btnSubmit.setOnClickListener(v -> {
            if (isLocked) {
                unlockAttendance();
            } else {
                submitAttendance();
            }
        });
    }

    private void loadStudents() {
        // Fetch ALL users
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, String> student = new HashMap<>();

                            student.put("uid", document.getId());
                            student.put("name", document.getString("name"));

                            // Safe Enrollment Fetch (Number or String)
                            Object enrollObj = document.get("enrollment");
                            student.put("enrollment", enrollObj != null ? String.valueOf(enrollObj) : "0");

                            studentList.add(student);
                        }
                        // --- SORT BY ENROLLMENT NUMBER ---
                        java.util.Collections.sort(studentList, (s1, s2) -> {
                            String e1 = s1.get("enrollment");
                            String e2 = s2.get("enrollment");

                            try {
                                Long l1 = Long.parseLong(e1);
                                Long l2 = Long.parseLong(e2);
                                return l1.compareTo(l2);
                            } catch (NumberFormatException e) {
                                return e1.compareTo(e2);
                            }
                        });
                        adapter.notifyDataSetChanged();
                        checkExistingAttendance();
                    }
                });
    }

    private void checkExistingAttendance() {
        db.collection("attendance").document(docId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Map<String, Object> data = document.getData();
                        if (data != null && data.containsKey("attendanceData")) {
                            Map<String, String> savedState = (Map<String, String>) data.get("attendanceData");
                            adapter.setAttendanceState(savedState);
                        }

                        if (data != null && data.containsKey("isLocked")) {
                            isLocked = Boolean.TRUE.equals(document.getBoolean("isLocked"));
                            updateLockButtonUI();
                        }
                    }
                });
    }

    private void submitAttendance() {
        Map<String, String> attendanceMap = adapter.getAttendanceState();

        if (attendanceMap.isEmpty()) {
            Toast.makeText(this, "Please mark attendance first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Saving...");

        // Prepare Data
        Map<String, Object> finalData = new HashMap<>();
        finalData.put("subject", subjectName);
        finalData.put("date", todayDate);
        finalData.put("attendanceData", attendanceMap);
        finalData.put("isLocked", true);

        // Save to Firebase: Collection 'attendance' -> Document '31-01-2026_Maths'
        db.collection("attendance").document(docId)
                .set(finalData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attendance Saved & Locked", Toast.LENGTH_SHORT).show();
                    isLocked = true;
                    updateLockButtonUI();

                    // --- NEW: Automatically mark Faculty as "Present" in Availability ---
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Map<String, Object> availabilityData = new HashMap<>();
                        availabilityData.put("status", "Present");

                        FirebaseFirestore.getInstance().collection("faculty").document(facultyUid)
                                .collection("availability").document(todayDate)
                                .set(availabilityData, SetOptions.merge());
                    }
                    // -------------------------------------------------------------------
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Attendance");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void unlockAttendance() {
        isLocked = false;

        Map<String, Object> unlockData = new HashMap<>();
        unlockData.put("isLocked", false);

        db.collection("attendance").document(docId)
                .set(unlockData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    updateLockButtonUI();
                    Toast.makeText(this, "Unlocked for Editing", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLockButtonUI() {
        btnSubmit.setEnabled(true);
        adapter.setLocked(isLocked);

        if (isLocked) {
            btnSubmit.setText("Edit Attendance");
            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            btnSubmit.setText("Submit Attendance");
            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C969E")));
        }
    }
}