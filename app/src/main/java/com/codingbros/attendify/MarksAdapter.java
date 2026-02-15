package com.codingbros.attendify;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarksAdapter extends RecyclerView.Adapter<MarksAdapter.ViewHolder> {

    private List<Map<String, String>> studentList;
    private boolean isLocked = false;

    // Key: StudentUID, Value: Marks Obtained
    private Map<String, String> marksState = new HashMap<>();

    public MarksAdapter(List<Map<String, String>> studentList) {
        this.studentList = studentList;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
        notifyDataSetChanged();
    }

    public Map<String, String> getMarksState() {
        return marksState;
    }

    public void setMarksState(Map<String, String> loadedState) {
        this.marksState = loadedState;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_marks_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> student = studentList.get(position);
        String uid = student.get("uid");
        String name = student.get("name");
        String enroll = student.get("enrollment");

        holder.tvRoll.setText(String.valueOf(position + 1));
        holder.tvEnroll.setText(enroll != null ? enroll : "N/A");
        holder.tvName.setText(name);

        // Remove listener before changing text to prevent infinite loops
        if (holder.textWatcher != null) {
            holder.etMarks.removeTextChangedListener(holder.textWatcher);
        }

        // Set value if exists
        if (marksState.containsKey(uid)) {
            holder.etMarks.setText(marksState.get(uid));
        } else {
            holder.etMarks.setText("");
        }

        // Lock State
        holder.etMarks.setEnabled(!isLocked);

        // Add Listener to save data as user types
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                marksState.put(uid, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        holder.etMarks.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvEnroll, tvName;
        EditText etMarks;
        TextWatcher textWatcher; // Keep reference to remove it later

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tv_roll_no);
            tvEnroll = itemView.findViewById(R.id.tv_enrollment);
            tvName = itemView.findViewById(R.id.tv_student_name);
            etMarks = itemView.findViewById(R.id.et_obtained_marks);
        }
    }
}