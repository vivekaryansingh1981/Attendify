package com.codingbros.attendify;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class NoticeBoardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvNoNotices;
    private FirebaseFirestore db;
    private NoticeAdapter adapter;
    private List<Map<String, String>> noticeList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_board);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_notices);
        tvNoNotices = findViewById(R.id.tv_no_notices);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoticeAdapter(noticeList);
        recyclerView.setAdapter(adapter);

        // Load Default Hardcoded Notices First
        loadDefaultNotices();

        // Optional: Fetch dynamic notices from Firebase to add to the list
        // fetchNotices();
    }

    private void loadDefaultNotices() {
        noticeList.clear();

        // 1. Default Notice: Vijay Trophy 2026
        Map<String, String> notice1 = new HashMap<>();
        notice1.put("title", "Vijay Trophy 2026 | Official Registration");
        notice1.put("desc", "Google Form Link: https://forms.gle/hk61empVbZ72cAQw6\n\n" +
                "Guidelines:\n" +
                "• Single Entry: Fill the form once only.\n" +
                "• Limit: Max 2 sports per student.\n" +
                "• Exception: Pharmacy, Arch & Design, Hospitality, and Law students can play up to 3 sports.\n" +
                "• Merged Teams: School of Architecture and Design will merge into unified teams.\n" +
                "• Strict Roster: No unauthorized substitution of team members.\n\n" +
                "Deadline: 24th February 2026.");
        notice1.put("author", "Sports Department");
        notice1.put("date", "19 Feb 2026");
        noticeList.add(notice1);

        // 2. Default Notice: Parent Meeting
        Map<String, String> notice2 = new HashMap<>();
        notice2.put("title", "Parent-Teacher Meeting (PTM)");
        notice2.put("desc", "A mandatory Parent-Teacher Meeting is scheduled for the upcoming weekend. The agenda includes discussion on academic progress, overall attendance, and preparation strategies for the upcoming Semester Exams. We kindly request all parents to attend.");
        notice2.put("author", "Administration");
        notice2.put("date", "15 Feb 2026");
        noticeList.add(notice2);

        adapter.notifyDataSetChanged();
        tvNoNotices.setVisibility(View.GONE);
    }

    private void fetchNotices() {
        // Assuming faculty adds notices to a collection named "notices"
        db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty() && noticeList.isEmpty()) {
                        tvNoNotices.setVisibility(View.VISIBLE);
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String author = doc.getString("author");

                        // Handle Date formatting
                        Long timestamp = doc.getLong("timestamp");
                        String dateStr = (timestamp != null) ? sdf.format(new Date(timestamp)) : "Recent";

                        Map<String, String> notice = new HashMap<>();
                        notice.put("title", title != null ? title : "Notice");
                        notice.put("desc", desc != null ? desc : "No description provided.");
                        notice.put("author", author != null ? author : "Admin");
                        notice.put("date", dateStr);

                        noticeList.add(notice);
                    }

                    adapter.notifyDataSetChanged();
                    tvNoNotices.setVisibility(noticeList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching notices: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}