package com.example.myshiftapp;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleResultAdapter extends RecyclerView.Adapter<ScheduleResultAdapter.CellVH> {

    private final List<String> days;
    private final List<String> shifts;
    private final int columns;
    private final List<String> cells;
    private final Map<String, String> assignments; // key -> fullName/UNASSIGNED

    public ScheduleResultAdapter(List<String> days,
                                 List<String> shifts,
                                 List<String> cells,
                                 Map<String, String> assignments) {
        this.days = days;
        this.shifts = shifts;
        this.cells = cells;
        this.columns = 1 + days.size();
        this.assignments = (assignments == null) ? new HashMap<>() : assignments;
    }

    @NonNull
    @Override
    public CellVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_cell, parent, false);
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

        String name = assignments.get(key);
        if (name == null || name.trim().isEmpty()) name = "UNASSIGNED";

        holder.tv.setText(name);

        if ("UNASSIGNED".equalsIgnoreCase(name)) {
            holder.tv.setBackgroundResource(R.drawable.bg_cell_empty); // Gray/Empty
        } else {
            holder.tv.setBackgroundResource(R.drawable.cell_green); // Green = There is an inlay
        }
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
