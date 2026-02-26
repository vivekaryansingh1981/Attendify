package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class StudTimetableAdapter extends RecyclerView.Adapter<StudTimetableAdapter.ViewHolder> {

    private List<Map<String, String>> classList;

    public StudTimetableAdapter(List<Map<String, String>> classList) {
        this.classList = classList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stud_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> cls = classList.get(position);

        String timeFrom = cls.containsKey("time_from") ? cls.get("time_from") : "";
        String timeTo = cls.containsKey("time_to") ? cls.get("time_to") : "";

        holder.tvTime.setText(timeFrom + " - " + timeTo);
        holder.tvSubject.setText(cls.get("subject_name"));

        // Match the exact key from EditTimetableActivity ("teacher_name")
        String facultyName = cls.get("teacher_name");

        if (facultyName != null && !facultyName.isEmpty()) {
            holder.tvTeacher.setText("Prof. " + facultyName);
            holder.tvTeacher.setVisibility(View.VISIBLE);
        } else {
            holder.tvTeacher.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSubject, tvTeacher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_class_time);
            tvSubject = itemView.findViewById(R.id.tv_class_subject);
            tvTeacher = itemView.findViewById(R.id.tv_teacher_name);
        }
    }
}