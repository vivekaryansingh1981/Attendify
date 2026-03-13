package com.codingbros.attendify;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ViewAttendanceAdapter extends RecyclerView.Adapter<ViewAttendanceAdapter.ViewHolder> {

    private List<Map<String, String>> studentList;

    public ViewAttendanceAdapter(List<Map<String, String>> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // --- FIXED: Create the row entirely from scratch so it never crashes! ---
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, 30, 0, 30); // Added a bit more padding for a cleaner look

        TextView tvEnroll = new TextView(parent.getContext());
        tvEnroll.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        tvEnroll.setGravity(Gravity.CENTER);
        tvEnroll.setTextSize(14f);

        TextView tvName = new TextView(parent.getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        tvName.setGravity(Gravity.CENTER);
        tvName.setTextSize(14f);

        TextView tvStatus = new TextView(parent.getContext());
        tvStatus.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTypeface(null, Typeface.BOLD);

        layout.addView(tvEnroll);
        layout.addView(tvName);
        layout.addView(tvStatus);

        return new ViewHolder(layout, tvEnroll, tvName, tvStatus);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> student = studentList.get(position);
        holder.tvName.setText(student.get("name"));
        holder.tvEnroll.setText(student.get("enrollment"));

        String status = student.containsKey("status") ? student.get("status") : "Not Marked";
        holder.tvStatus.setText(status);

        if ("Present".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else if ("Absent".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#E53935")); // Red
        } else {
            holder.tvStatus.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() { return studentList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEnroll, tvName, tvStatus;

        // Pass the dynamically created views directly into the ViewHolder
        public ViewHolder(@NonNull View itemView, TextView tvEnroll, TextView tvName, TextView tvStatus) {
            super(itemView);
            this.tvEnroll = tvEnroll;
            this.tvName = tvName;
            this.tvStatus = tvStatus;
        }
    }
}