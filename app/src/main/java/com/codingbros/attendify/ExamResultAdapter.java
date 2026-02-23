package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ExamResultAdapter extends RecyclerView.Adapter<ExamResultAdapter.ViewHolder> {

    private List<Map<String, String>> list;

    public ExamResultAdapter(List<Map<String, String>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        holder.tvExamName.setText(item.get("exam"));
        holder.tvObtained.setText(item.get("obtained"));
        holder.tvTotal.setText(item.get("total"));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExamName, tvObtained, tvTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExamName = itemView.findViewById(R.id.tv_exam_name);
            tvObtained = itemView.findViewById(R.id.tv_obtained);
            tvTotal = itemView.findViewById(R.id.tv_total);
        }
    }
}