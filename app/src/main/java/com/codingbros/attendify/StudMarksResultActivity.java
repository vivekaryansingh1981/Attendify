package com.codingbros.attendify;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudMarksResultActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvTitle, tvNoData;
    private FirebaseFirestore db;
    private String subjectName, studUid;
    private ExamResultAdapter adapter; // Using the ExamResultAdapter you already made
    private List<Map<String, String>> resultList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studmarks_result);

        subjectName = getIntent().getStringExtra("subject_name");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tv_subject_title);
        tvNoData = findViewById(R.id.tv_no_data);
        recyclerView = findViewById(R.id.recycler_results);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (subjectName != null) {
            tvTitle.setText(subjectName + " Results");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExamResultAdapter(resultList);
        recyclerView.setAdapter(adapter);

        fetchStudentMarks();
    }

    private void fetchStudentMarks() {
        if (studUid == null) return;

        // Query the marks collection for this specific subject
        db.collection("marks")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    resultList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();

                        if (data.containsKey("marks_data")) {
                            Map<String, String> marksMap = (Map<String, String>) data.get("marks_data");

                            // Find the specific student's UID in the marks_data map
                            if (marksMap != null && marksMap.containsKey(studUid)) {
                                String obtained = marksMap.get(studUid);
                                String total = (String) data.get("total_marks");
                                String examName = (String) data.get("exam_name");

                                Map<String, String> resultItem = new HashMap<>();
                                resultItem.put("exam", examName != null ? examName : "Exam");
                                resultItem.put("obtained", obtained != null ? obtained : "0");
                                resultItem.put("total", total != null ? total : "0");

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