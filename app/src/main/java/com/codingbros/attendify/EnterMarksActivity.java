package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EnterMarksActivity extends AppCompatActivity {

    private LinearLayout containerSubjects;
    private FirebaseFirestore db;
    private String facultyUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_marks);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        containerSubjects = findViewById(R.id.container_subjects);
        ImageView btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        loadSubjects();
    }

    private void loadSubjects() {
        db.collection("faculty").document(facultyUid)
                .collection("subjects")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerSubjects.removeAllViews();

                        if(task.getResult().isEmpty()){
                            TextView tv = new TextView(this);
                            tv.setText("No subjects found.\nPlease add subjects in the Attendance section first.");
                            tv.setPadding(50,50,50,50);
                            tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
                            containerSubjects.addView(tv);
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String abbr = document.getString("abbr");
                            addSubjectView(name, abbr, document.getId());
                        }
                    }
                });
    }

    private void addSubjectView(String name, String abbr, String docId) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_subject_card, containerSubjects, false);

        TextView tvName = view.findViewById(R.id.tv_subject_name);
        TextView tvAbbr = view.findViewById(R.id.tv_subject_abbr);
        LinearLayout layoutStats = view.findViewById(R.id.layout_attendance_stats);

        tvName.setText(name);
        tvAbbr.setText(abbr);

        // --- FIXED: Explicitly hide the attendance stats block here ---
        layoutStats.setVisibility(View.GONE);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentMarksActivity.class);
            intent.putExtra("subject_name", name);
            intent.putExtra("subject_abbr", abbr);
            startActivity(intent);
        });

        containerSubjects.addView(view);
    }
}