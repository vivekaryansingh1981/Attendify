package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudAttendanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String studUid;

    private StudSubjectAttendanceAdapter adapter;
    private List<Map<String, String>> subjectList = new ArrayList<>();
    private Set<String> uniqueSubjects = new HashSet<>();

    // Key = Subject Name, Value = int[] {presentCount, totalCount}
    private Map<String, int[]> attendanceStats = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studattendance);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        recyclerView = findViewById(R.id.recycler_subjects);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudSubjectAttendanceAdapter(subjectList, this::onSubjectClick);
        recyclerView.setAdapter(adapter);

        calculateStudentAttendance();
    }

    private void calculateStudentAttendance() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("attendance").get().addOnSuccessListener(queryDocumentSnapshots -> {

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String subjectName = doc.getString("subject");

                if (subjectName != null && doc.contains("attendanceData")) {
                    Map<String, String> attData = (Map<String, String>) doc.get("attendanceData");

                    if (attData != null && attData.containsKey(studUid)) {
                        String status = attData.get(studUid);

                        attendanceStats.putIfAbsent(subjectName, new int[]{0, 0});
                        attendanceStats.get(subjectName)[1]++;

                        if ("Present".equalsIgnoreCase(status)) {
                            attendanceStats.get(subjectName)[0]++;
                        }
                    }
                }
            }

            loadFacultySubjects();

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error fetching attendance", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadFacultySubjects() {
        db.collection("faculty").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectList.clear();
            uniqueSubjects.clear();

            if (queryDocumentSnapshots.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                return;
            }

            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                doc.getReference().collection("subjects").get()
                        .addOnSuccessListener(subjectSnaps -> {

                            for (DocumentSnapshot subDoc : subjectSnaps) {
                                String name = subDoc.getString("name");
                                String abbr = subDoc.getString("abbr");

                                if (name != null && !uniqueSubjects.contains(name)) {
                                    uniqueSubjects.add(name);

                                    int present = 0;
                                    int total = 0;
                                    int percentage = 0;

                                    if (attendanceStats.containsKey(name)) {
                                        present = attendanceStats.get(name)[0];
                                        total = attendanceStats.get(name)[1];
                                        if (total > 0) {
                                            percentage = (present * 100) / total;
                                        }
                                    }

                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("name", name);
                                    map.put("abbr", abbr != null ? abbr : "");
                                    map.put("present", String.valueOf(present));
                                    map.put("total", String.valueOf(total));
                                    map.put("percentage", String.valueOf(percentage));

                                    subjectList.add(map);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                        });
            }
        });
    }

    private void onSubjectClick(String subjectName) {
        // --- FIXED: Find the abbreviation before sending the Intent ---
        String subjectAbbr = subjectName; // Default fallback
        for (Map<String, String> map : subjectList) {
            if (subjectName.equals(map.get("name"))) {
                subjectAbbr = map.get("abbr");
                break;
            }
        }

        Intent intent = new Intent(this, StudAttendanceDetailActivity.class);
        intent.putExtra("subject_name", subjectName);
        intent.putExtra("subject_abbr", subjectAbbr); // Pack it!
        startActivity(intent);
    }
}