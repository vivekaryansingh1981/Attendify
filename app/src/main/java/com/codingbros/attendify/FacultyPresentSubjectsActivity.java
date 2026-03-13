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

public class FacultyPresentSubjectsActivity extends AppCompatActivity {

    private LinearLayout containerSubjects;
    private FirebaseFirestore db;
    private String facultyUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_present_subjects);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        containerSubjects = findViewById(R.id.container_subjects);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadSubjects();
    }

    private void loadSubjects() {
        db.collection("faculty").document(facultyUid).collection("subjects").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerSubjects.removeAllViews();
                        if(task.getResult().isEmpty()){
                            TextView tv = new TextView(this);
                            tv.setText("No subjects found.");
                            tv.setPadding(50,50,50,50);
                            containerSubjects.addView(tv);
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            addSubjectView(document.getString("name"), document.getString("abbr"));
                        }
                    }
                });
    }

    private void addSubjectView(String name, String abbr) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_subject_card, containerSubjects, false);

        TextView tvName = view.findViewById(R.id.tv_subject_name);
        TextView tvAbbr = view.findViewById(R.id.tv_subject_abbr);
        view.findViewById(R.id.layout_attendance_stats).setVisibility(View.GONE);

        tvName.setText(name);
        tvAbbr.setText(abbr);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyPresentListActivity.class);
            intent.putExtra("subject_name", name);
            intent.putExtra("subject_abbr", abbr);
            startActivity(intent);
        });

        containerSubjects.addView(view);
    }
}