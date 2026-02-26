package com.codingbros.attendify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class StudSubjectAttendanceAdapter extends RecyclerView.Adapter<StudSubjectAttendanceAdapter.ViewHolder> {

    private List<Map<String, String>> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(String subjectName);
    }

    public StudSubjectAttendanceAdapter(List<Map<String, String>> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stud_subject_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);

        String name = item.get("name");
        holder.tvName.setText(name);
        holder.tvAbbr.setText(item.get("abbr"));

        int present = Integer.parseInt(item.get("present"));
        int total = Integer.parseInt(item.get("total"));
        int percentage = Integer.parseInt(item.get("percentage"));

        holder.tvPercentage.setText(percentage + "%");
        holder.progressBar.setProgress(percentage);
        holder.tvStats.setText(present + " / " + total + " Classes Attended");

        // Change color to red if attendance is below 75%
        if (percentage < 75 && total > 0) {
            holder.tvPercentage.setTextColor(Color.parseColor("#E53935"));
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
        } else {
            holder.tvPercentage.setTextColor(Color.parseColor("#4CB5C3"));
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CB5C3")));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(name));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAbbr, tvPercentage, tvStats;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_subject_name);
            tvAbbr = itemView.findViewById(R.id.tv_subject_abbr);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            tvStats = itemView.findViewById(R.id.tv_stats);
            progressBar = itemView.findViewById(R.id.progress_subject_attendance);
        }
    }
}