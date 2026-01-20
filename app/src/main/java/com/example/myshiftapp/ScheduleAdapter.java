package com.example.myshiftapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.CellVH> {

    public interface OnCellToggleListener {
        void onToggle(String key, Boolean newValue); // key like "Sun_Morning"
    }

    private final Context context;
    private final List<String> days;
    private final List<String> shifts;
    private final int columns;
    private final List<String> cells; // flattened table (including headers)
    private final Map<String, Boolean> availability; // key -> true/false
    private final boolean canEdit;
    private final OnCellToggleListener listener;

    public ScheduleAdapter(
            Context context,
            List<String> days,
            List<String> shifts,
            List<String> cells,
            Map<String, Boolean> availability,
            boolean canEdit,
            OnCellToggleListener listener
    ) {
        this.context = context;
        this.days = days;
        this.shifts = shifts;
        this.cells = cells;
        this.availability = (availability == null) ? new HashMap<>() : availability;
        this.columns = 1 + days.size();
        this.canEdit = canEdit;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CellVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_cell, parent, false);
        return new CellVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CellVH holder, int position) {
        String cellText = cells.get(position);
        holder.tv.setText(cellText);

        int row = position / columns;
        int col = position % columns;

        boolean isHeader = (row == 0 || col == 0);

        // Default style
        holder.tv.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        holder.tv.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        if (isHeader) {
            // Header cells (top row / first column)
            holder.tv.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            holder.tv.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.tv.setOnClickListener(null);
            return;
        }

        // Real cell => build key: "Sun_Morning"
        String day = days.get(col - 1);
        String shift = shifts.get(row - 1);
        String key = day + "_" + shift;

        Boolean val = availability.get(key);

        if (val == null) {
            // Not selected yet
            holder.tv.setText(""); // keep clean
            holder.tv.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        } else if (val) {
            // Available
            holder.tv.setText("✓");
            holder.tv.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light));
        } else {
            // Not available
            holder.tv.setText("X");
            holder.tv.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
        }

        holder.tv.setOnClickListener(v -> {
            if (!canEdit) return;

            // Cycle: null -> true -> false -> true ...
            Boolean newVal;
            if (val == null) newVal = true;
            else newVal = !val;

            availability.put(key, newVal);
            notifyItemChanged(position);

            if (listener != null) listener.onToggle(key, newVal);
        });
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
