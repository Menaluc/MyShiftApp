package com.example.myshiftapp;

import android.content.Context;
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

//we clean first.
        holder.tv.setOnClickListener(null);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setClickable(false);
        holder.tv.setTypeface(null, Typeface.NORMAL);

        if (isHeader) {
            holder.tv.setBackgroundResource(R.drawable.bg_cell_header);
            holder.tv.setTypeface(null, Typeface.BOLD);
            return;
        }

// Real cell => key: "Sun_Morning"
        String day = days.get(col - 1);
        String shift = shifts.get(row - 1);
        String key = day + "_" + shift;

        Boolean val = availability.get(key);

        if (val == null) {
            holder.tv.setText("");
            holder.tv.setBackgroundResource(R.drawable.bg_cell_empty);
        } else if (val) {
            holder.tv.setText("✓");
            holder.tv.setBackgroundResource(R.drawable.cell_green);
        } else {
            holder.tv.setText("X");
            holder.tv.setBackgroundResource(R.drawable.cell_red);
        }

     // click on the entire cell, not just the TextView
        holder.itemView.setClickable(true);
        holder.itemView.setOnClickListener(v -> {
            if (!canEdit) return;

            Boolean currentVal = availability.get(key);
            Boolean newVal;
            if (currentVal == null) newVal = true;
            else newVal = !currentVal;

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