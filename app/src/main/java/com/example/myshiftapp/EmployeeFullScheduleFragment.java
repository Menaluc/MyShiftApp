package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeFullScheduleFragment extends Fragment {

    private FirebaseFirestore db;

    private TextView tvSummary;
    private RecyclerView rv;

    private Button btnBack;

    public EmployeeFullScheduleFragment() {
        super(R.layout.fragment_employee_full_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        tvSummary = view.findViewById(R.id.tvSummary);
        rv = view.findViewById(R.id.rvFullSchedule);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        loadAll();
    }

    private void loadAll() {
// 1) Get weekId מ-scheduleConfig/currentWeek
        db.collection("scheduleConfig").document("currentWeek")
                .get()
                .addOnSuccessListener(cfg -> {

                    String weekIdRaw = cfg.getString("currentWeekId");
                    final String weekId = TextUtils.isEmpty(weekIdRaw) ? "currentWeek" : weekIdRaw;

                    Boolean pub = cfg.getBoolean("schedulePublished");
                    boolean isPublished = (pub != null && pub);

                    if (!isPublished) {
                        tvSummary.setText("Schedule not published yet (weekId: " + weekId + ")");
                        setupEmptyTableDefault();
                        return;
                    }

//  2) Get shift_config for days/shifts
                    db.collection("settings").document("shift_config")
                            .get()
                            .addOnSuccessListener(shiftCfg -> {
                                String workDays = shiftCfg.getString("workDays");
                                String shiftType = shiftCfg.getString("shiftType");
                                if (workDays == null) workDays = "SunThu";
                                if (shiftType == null) shiftType = "2";

                                List<String> days = buildDays(workDays);
                                List<String> shifts = buildShifts(shiftType);

//3) Get builtSchedules/weekId
                                loadBuiltScheduleAndRender(weekId, days, shifts);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(),
                                        "Failed to load shift settings. Using default days/shifts.",
                                        Toast.LENGTH_SHORT).show();

                                List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
                                List<String> shifts = Arrays.asList("Morning", "Evening");
                                loadBuiltScheduleAndRender(weekId, days, shifts);
                            });
                })
                .addOnFailureListener(e -> {
                    tvSummary.setText("Failed to load scheduleConfig: " + e.getMessage());
                    setupEmptyTableDefault();
                });
    }

    private void loadBuiltScheduleAndRender(final String weekId, List<String> days, List<String> shifts) {
        db.collection("builtSchedules").document(weekId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        tvSummary.setText("No built schedule document found for weekId: " + weekId);
                        renderTable(days, shifts, new HashMap<>());
                        return;
                    }

                    Object raw = doc.get("assignments");
                    Map<String, Object> assignments = new HashMap<>();
                    if (raw instanceof Map) {
//noinspection unchecked
                        assignments = (Map<String, Object>) raw;
                    }

                    tvSummary.setText("Full schedule for weekId: " + weekId);
                    renderTable(days, shifts, assignments);
                })
                .addOnFailureListener(e -> {
                    tvSummary.setText("Failed to load built schedule: " + e.getMessage());
                    renderTable(days, shifts, new HashMap<>());
                });
    }

    private void renderTable(List<String> days, List<String> shifts, Map<String, Object> assignments) {
        List<String> cells = buildCells(days, shifts);
        int columns = 1 + days.size();

        rv.setLayoutManager(new GridLayoutManager(requireContext(), columns));

        EmployeeFullScheduleFragmentAdapter adapter = new EmployeeFullScheduleFragmentAdapter(
                requireContext(),
                days,
                shifts,
                cells,
                assignments
        );

        rv.setAdapter(adapter);
    }

    private void setupEmptyTableDefault() {
        List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
        List<String> shifts = Arrays.asList("Morning", "Evening");
        renderTable(days, shifts, new HashMap<>());
    }

    private List<String> buildCells(List<String> days, List<String> shifts) {
        List<String> cells = new ArrayList<>();
        cells.add("");
        cells.addAll(days);

        for (String shift : shifts) {
            cells.add(shift);
            for (int i = 0; i < days.size(); i++) cells.add("");
        }
        return cells;
    }

    private List<String> buildDays(String workDays) {
        if ("SunFri".equalsIgnoreCase(workDays)) {
            return Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu", "Fri");
        } else if ("SunSat".equalsIgnoreCase(workDays)) {
            return Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat");
        } else {
            return Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
        }
    }

    private List<String> buildShifts(String shiftType) {
        if ("3".equals(shiftType)) {
            return Arrays.asList("Morning", "Evening", "Night");
        } else {
            return Arrays.asList("Morning", "Evening");
        }
    }
}