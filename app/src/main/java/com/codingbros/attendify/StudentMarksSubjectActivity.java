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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentMarksSubjectActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private StudentSubjectAdapter adapter; // Reuse existing adapter
    private List<Map<String, String>> subjectList = new ArrayList<>();
    private Set<String> uniqueSubjects = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_marks_subject);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_subjects);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentSubjectAdapter(subjectList, this::onSubjectClick);
        recyclerView.setAdapter(adapter);

        loadAllFacultySubjects();
    }

    private void loadAllFacultySubjects() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("faculty").get().addOnSuccessListener(queryDocumentSnapshots -> {
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
        });
    }

    private void onSubjectClick(String subjectName) {
        // Go to Result Activity
        Intent intent = new Intent(this, StudentMarksResultActivity.class);
        intent.putExtra("subject_name", subjectName);
        startActivity(intent);
    }
}