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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentAttendanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private StudentSubjectAdapter adapter;
    private List<Map<String, String>> subjectList = new ArrayList<>();
    private Set<String> uniqueSubjects = new HashSet<>(); // To avoid duplicates

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_subjects);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // We reuse the adapter logic, but simple inner class here for ease
        adapter = new StudentSubjectAdapter(subjectList, this::onSubjectClick);
        recyclerView.setAdapter(adapter);

        loadAllFacultySubjects();
    }

    private void loadAllFacultySubjects() {
        progressBar.setVisibility(View.VISIBLE);

        // 1. Get All Faculty Members
        db.collection("faculty").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "No Faculty Found", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. For each faculty, get their 'subjects' subcollection
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                doc.getReference().collection("subjects").get()
                        .addOnSuccessListener(subjectSnaps -> {
                            for (DocumentSnapshot subDoc : subjectSnaps) {
                                String name = subDoc.getString("name");
                                String abbr = subDoc.getString("abbr");

                                // Prevent duplicates if multiple teachers teach same subject name
                                if (name != null && !uniqueSubjects.contains(name)) {
                                    uniqueSubjects.add(name);

                                    // Use a Map compatible with your existing structures
                                    java.util.HashMap<String, String> map = new java.util.HashMap<>();
                                    map.put("name", name);
                                    map.put("abbr", abbr);
                                    subjectList.add(map);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                        });
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
        });
    }

    private void onSubjectClick(String subjectName) {
        Intent intent = new Intent(this, StudentAttendanceDetailActivity.class);
        intent.putExtra("subject_name", subjectName);
        startActivity(intent);
    }
}