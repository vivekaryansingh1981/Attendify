package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    private List<Map<String, String>> list;

    public NoticeAdapter(List<Map<String, String>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        holder.tvTitle.setText(item.get("title"));
        holder.tvDesc.setText(item.get("desc"));
        holder.tvAuthor.setText("Posted by: " + item.get("author"));
        holder.tvDate.setText(item.get("date"));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvAuthor, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notice_title);
            tvDesc = itemView.findViewById(R.id.tv_notice_desc);
            tvAuthor = itemView.findViewById(R.id.tv_notice_author);
            tvDate = itemView.findViewById(R.id.tv_notice_date);
        }
    }
}