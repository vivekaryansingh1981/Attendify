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

        // Fetch dynamic notices directly from Firebase
        fetchNotices();
    }

    private void fetchNotices() {
        // Fetch notices from the "notices" collection, ordered by newest first
        db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    noticeList.clear(); // Clear list to prevent duplicates on reload

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoNotices.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String author = doc.getString("author");

                        // Handle Date formatting safely
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