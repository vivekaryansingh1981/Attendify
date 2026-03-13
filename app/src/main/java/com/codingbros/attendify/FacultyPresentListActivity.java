package com.codingbros.attendify;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyPresentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ViewAttendanceAdapter adapter;
    private List<Map<String, String>> studentList = new ArrayList<>();
    private List<DocumentSnapshot> historyDocs = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvDate;
    private String subjectName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_present_list);

        subjectName = getIntent().getStringExtra("subject_name");
        String subjectAbbr = getIntent().getStringExtra("subject_abbr");
        if (subjectAbbr == null) subjectAbbr = subjectName;

        db = FirebaseFirestore.getInstance();

        // --- FIXED: Safely setup the toolbar without crashing the theme ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_present_students);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_history) {
                showHistoryDialog();
                return true;
            }
            return false;
        });

        TextView tvSubjectTitle = findViewById(R.id.tv_subject_title);
        tvDate = findViewById(R.id.tv_current_date);
        tvSubjectTitle.setText(subjectAbbr);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_students);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ViewAttendanceAdapter(studentList);
        recyclerView.setAdapter(adapter);

        loadStudentsBase();
    }

    private void loadStudentsBase() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                studentList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, String> student = new HashMap<>();
                    student.put("uid", document.getId());
                    student.put("name", document.getString("name"));
                    Object enrollObj = document.get("enrollment");
                    student.put("enrollment", enrollObj != null ? String.valueOf(enrollObj) : "0");
                    student.put("status", "-"); // Default empty status
                    studentList.add(student);
                }

                // Sort by enrollment
                Collections.sort(studentList, (s1, s2) -> {
                    try { return Long.valueOf(s1.get("enrollment")).compareTo(Long.valueOf(s2.get("enrollment"))); }
                    catch (NumberFormatException e) { return s1.get("enrollment").compareTo(s2.get("enrollment")); }
                });

                fetchAttendanceHistory();
            }
        });
    }

    private void fetchAttendanceHistory() {
        db.collection("attendance").whereEqualTo("subject", subjectName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyDocs.clear();
                    historyDocs.addAll(queryDocumentSnapshots.getDocuments());

                    if (historyDocs.isEmpty()) {
                        tvDate.setText("No attendance records found.");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    // Sort documents by Date parsed from string (Newest first)
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    Collections.sort(historyDocs, (d1, d2) -> {
                        try {
                            Date date1 = sdf.parse(d1.getString("date"));
                            Date date2 = sdf.parse(d2.getString("date"));
                            return date2.compareTo(date1); // Descending
                        } catch (Exception e) { return 0; }
                    });

                    // Auto-load the most recent date
                    loadDataFromDoc(historyDocs.get(0));
                });
    }

    private void showHistoryDialog() {
        if (historyDocs.isEmpty()) {
            Toast.makeText(this, "No previous data found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] datesArray = new String[historyDocs.size()];
        for (int i = 0; i < historyDocs.size(); i++) {
            datesArray[i] = historyDocs.get(i).getString("date");
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Date")
                .setItems(datesArray, (dialog, which) -> {
                    loadDataFromDoc(historyDocs.get(which));
                })
                .show();
    }

    private void loadDataFromDoc(DocumentSnapshot doc) {
        String docDate = doc.getString("date");
        tvDate.setText("Date: " + docDate);

        Map<String, String> attendanceData = (Map<String, String>) doc.get("attendanceData");

        // Apply statuses to our student list
        for (Map<String, String> student : studentList) {
            String uid = student.get("uid");
            if (attendanceData != null && attendanceData.containsKey(uid)) {
                student.put("status", attendanceData.get(uid));
            } else {
                student.put("status", "Not Marked");
            }
        }
        adapter.notifyDataSetChanged();
    }
}