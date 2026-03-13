package com.codingbros.attendify;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyManageNoticesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvNoNotices;
    private FirebaseFirestore db;
    private ManageNoticeAdapter adapter;
    private List<Map<String, String>> noticeList = new ArrayList<>();

    private String facultyUid;
    private String facultyName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_manage_notices);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        recyclerView = findViewById(R.id.recycler_manage_notices);
        tvNoNotices = findViewById(R.id.tv_no_notices);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageNoticeAdapter(noticeList, new ManageNoticeAdapter.OnNoticeClickListener() {
            @Override
            public void onEdit(Map<String, String> notice) {
                // Pass data back to FacultySendNoticeActivity for editing
                Intent intent = new Intent(FacultyManageNoticesActivity.this, FacultySendNoticeActivity.class);
                intent.putExtra("notice_id", notice.get("id"));
                intent.putExtra("notice_title", notice.get("title"));
                intent.putExtra("notice_desc", notice.get("description"));
                startActivity(intent);
            }

            @Override
            public void onDelete(String noticeId, int position) {
                confirmDelete(noticeId, position);
            }
        });

        recyclerView.setAdapter(adapter);

        fetchFacultyNameAndNotices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when coming back from editing
        if (!facultyName.isEmpty()) {
            loadMyNotices();
        }
    }

    private void fetchFacultyNameAndNotices() {
        if (facultyUid == null) return;
        db.collection("faculty").document(facultyUid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getString("name") != null) {
                facultyName = doc.getString("name");
            } else {
                facultyName = "Faculty"; // Fallback
            }
            loadMyNotices();
        });
    }

    private void loadMyNotices() {
        // Fetch ALL notices, order by time, and filter locally by Author name to avoid Firebase Index Crash errors
        db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    noticeList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

                    String authorTag = "Prof. " + facultyName;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String author = doc.getString("author");

                        // Only show notices posted by THIS faculty member
                        if (author != null && author.equals(authorTag)) {
                            Map<String, String> notice = new HashMap<>();
                            notice.put("id", doc.getId());
                            notice.put("title", doc.getString("title"));
                            notice.put("description", doc.getString("description"));
                            notice.put("author", author);
                            Long timestamp = doc.getLong("timestamp");
                            notice.put("date", (timestamp != null) ? sdf.format(new Date(timestamp)) : "Recent");

                            noticeList.add(notice);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    tvNoNotices.setVisibility(noticeList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching notices", Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(String noticeId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("notices").document(noticeId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Notice Deleted", Toast.LENGTH_SHORT).show();
                                noticeList.remove(position);
                                adapter.notifyItemRemoved(position);
                                if (noticeList.isEmpty()) tvNoNotices.setVisibility(View.VISIBLE);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}