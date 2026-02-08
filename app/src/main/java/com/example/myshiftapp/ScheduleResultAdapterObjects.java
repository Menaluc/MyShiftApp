package com.example.myshiftapp;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleResultAdapterObjects extends RecyclerView.Adapter<ScheduleResultAdapterObjects.CellVH> {

    private final List<String> days;
    private final List<String> shifts;
    private final int columns;
    private final List<String> cells;

    // key -> {fullName, uid} OR null
    private final Map<String, Object> assignments;

    public ScheduleResultAdapterObjects(List<String> days,
                                        List<String> shifts,
                                        List<String> cells,
                                        Map<String, Object> assignments) {
        this.days = days;
        this.shifts = shifts;
        this.cells = cells;
        this.columns = 1 + days.size();
        this.assignments = (assignments == null) ? new HashMap<>() : assignments;
    }

    @NonNull
    @Override
    public CellVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_cell, parent, false);
        return new CellVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CellVH holder, int position) {
        int row = position / columns;
        int col = position % columns;

        boolean isHeader = (row == 0 || col == 0);

        holder.tv.setOnClickListener(null);
        holder.tv.setTypeface(null, Typeface.NORMAL);

        if (isHeader) {
            holder.tv.setText(cells.get(position));
            holder.tv.setBackgroundResource(R.drawable.bg_cell_header);
            holder.tv.setTypeface(null, Typeface.BOLD);
            return;
        }

        String day = days.get(col - 1);
        String shift = shifts.get(row - 1);
        String key = day + "_" + shift;

        String name = extractFullName(assignments.get(key));
        if (TextUtils.isEmpty(name)) {
            holder.tv.setText("UNASSIGNED");
            holder.tv.setBackgroundResource(R.drawable.bg_cell_empty);
        } else {
            holder.tv.setText(name);
            holder.tv.setBackgroundResource(R.drawable.cell_green);
        }
    }

    private String extractFullName(Object raw) {
        if (!(raw instanceof Map)) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) raw;

        Object fn = m.get("fullName");
        return (fn == null) ? null : String.valueOf(fn);
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class CellVH extends RecyclerView.ViewHolder {
        TextView tv;
        CellVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvCell);
        }
    }
}