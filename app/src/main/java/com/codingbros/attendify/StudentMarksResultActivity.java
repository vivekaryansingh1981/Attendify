package com.codingbros.attendify;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentMarksResultActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvTitle, tvNoData;
    private FirebaseFirestore db;
    private String subjectName, studentUid;
    private ExamResultAdapter adapter;
    private List<Map<String, String>> resultList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_marks_result);

        subjectName = getIntent().getStringExtra("subject_name");
        studentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tv_subject_title);
        tvNoData = findViewById(R.id.tv_no_data);
        recyclerView = findViewById(R.id.recycler_results);

        tvTitle.setText(subjectName + " Results");
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExamResultAdapter(resultList);
        recyclerView.setAdapter(adapter);

        fetchMarks();
    }

    private void fetchMarks() {
        // Query marks collection where subject matches
        db.collection("marks")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                        return;
                    }

                    resultList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();

                        // Check if marks_data exists
                        if (data.containsKey("marks_data")) {
                            Map<String, String> marksMap = (Map<String, String>) data.get("marks_data");

                            // Check if CURRENT STUDENT has marks in this exam
                            if (marksMap != null && marksMap.containsKey(studentUid)) {
                                String obtained = marksMap.get(studentUid);
                                String total = (String) data.get("total_marks");
                                String examName = (String) data.get("exam_name");

                                Map<String, String> resultItem = new java.util.HashMap<>();
                                resultItem.put("exam", examName);
                                resultItem.put("obtained", obtained);
                                resultItem.put("total", total);

                                resultList.add(resultItem);
                            }
                        }
                    }

                    if (resultList.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                        tvNoData.setText("No marks found for you in this subject.");
                    } else {
                        tvNoData.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching marks", Toast.LENGTH_SHORT).show());
    }
}