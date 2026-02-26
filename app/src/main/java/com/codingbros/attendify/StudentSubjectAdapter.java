package com.codingbros.attendify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class StudentSubjectAdapter extends RecyclerView.Adapter<StudentSubjectAdapter.ViewHolder> {

    private List<Map<String, String>> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(String subjectName);
    }

    public StudentSubjectAdapter(List<Map<String, String>> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        holder.tvName.setText(item.get("name"));
        holder.tvAbbr.setText(item.get("abbr"));

        // DYNAMIC ATTENDANCE UI CHECK
        if (item.containsKey("percentage") && item.containsKey("present") && item.containsKey("total")) {
            holder.layoutStats.setVisibility(View.VISIBLE);

            String percentageStr = item.get("percentage");
            String present = item.get("present");
            String total = item.get("total");

            holder.tvPercentage.setText(percentageStr + "%");
            holder.tvClassesConducted.setText(present + "/" + total + " Classes Conducted");

            try {
                int perc = Integer.parseInt(percentageStr);
                holder.progressBar.setProgress(perc);

                // --- COLOR CODING LOGIC ---
                if (perc < 50) {
                    // RED for below 50%
                    holder.tvPercentage.setTextColor(Color.parseColor("#E53935"));
                    holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
                } else if (perc < 75) {
                    // ORANGE/YELLOW for 50% to 74% (Darker yellow/orange for better visibility)
                    holder.tvPercentage.setTextColor(Color.parseColor("#F57C00"));
                    holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F57C00")));
                } else {
                    // GREEN for 75% and above
                    holder.tvPercentage.setTextColor(Color.parseColor("#43A047"));
                    holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#43A047")));
                }

            } catch (NumberFormatException e) {
                holder.progressBar.setProgress(0);
                holder.tvPercentage.setTextColor(Color.parseColor("#0C7779")); // Default color
                holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#0C7779")));
            }
        } else {
            // Hide the Attendance UI completely for Marks page
            holder.layoutStats.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(item.get("name")));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAbbr;
        LinearLayout layoutStats;
        ProgressBar progressBar;
        TextView tvPercentage, tvClassesConducted;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_subject_name);
            tvAbbr = itemView.findViewById(R.id.tv_subject_abbr);

            layoutStats = itemView.findViewById(R.id.layout_attendance_stats);
            progressBar = itemView.findViewById(R.id.progress_attendance);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            tvClassesConducted = itemView.findViewById(R.id.tv_classes_conducted);
        }
    }
}