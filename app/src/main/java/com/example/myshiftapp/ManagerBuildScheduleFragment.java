package com.example.myshiftapp;

import android.text.TextUtils;
import android.os.Bundle;
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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManagerBuildScheduleFragment extends Fragment {

    private FirebaseFirestore db;

    private TextView tvSummary;
    private Button btnBuild;
    private Button btnPublish;
    private RecyclerView rvTable;

    // state
    private String weekId = "currentWeek";
    private boolean submissionOpen = false;
    private long deadlineMillis = 0L;
    private boolean scheduleBuilt = false;
    private boolean schedulePublished = false;

    private List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
    private List<String> shifts = Arrays.asList("Morning", "Evening");

    // last build results (for publish / save)
    private Map<String, String> lastScheduleDisplay = new LinkedHashMap<>();
    private Map<String, Object> lastAssignmentsToSave = new LinkedHashMap<>();

    public ManagerBuildScheduleFragment() {
        super(R.layout.fragment_manager_build_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        tvSummary = view.findViewById(R.id.tvSummary);
        btnBuild = view.findViewById(R.id.btnBuild);
        btnPublish = view.findViewById(R.id.btnPublish);
        rvTable = view.findViewById(R.id.rvScheduleResult);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        btnBuild.setOnClickListener(v -> buildNow());
        btnPublish.setOnClickListener(v -> publishNow());

//Inactive until information is loaded
        btnBuild.setEnabled(false);
        btnPublish.setEnabled(false);

        loadConfigThenSettings();
    }

    // =========================
// 1) Load config + settings
// =========================
    private void loadConfigThenSettings() {
        tvSummary.setText("Loading config...");
        db.collection("scheduleConfig").document("currentWeek")
                .get()
                .addOnSuccessListener(cfg -> {
                    String w = cfg.getString("currentWeekId");
                    if (!TextUtils.isEmpty(w)) weekId = w.trim();

                    Boolean open = cfg.getBoolean("submissionOpen");
                    submissionOpen = (open != null && open);

                    Long dl = cfg.getLong("submissionDeadlineMillis");
                    deadlineMillis = (dl == null) ? 0L : dl;

                    Boolean built = cfg.getBoolean("scheduleBuilt");
                    scheduleBuilt = (built != null && built);

                    Boolean pub = cfg.getBoolean("schedulePublished");
                    schedulePublished = (pub != null && pub);

                    loadShiftSettings();
                })
                .addOnFailureListener(e -> {
                    tvSummary.setText("Failed to load scheduleConfig: " + e.getMessage());
                    loadShiftSettings();
                });
    }

    private void loadShiftSettings() {
        tvSummary.setText("Loading settings...");

        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(doc -> {
                    String workDays = doc.getString("workDays");
                    String shiftType = doc.getString("shiftType");

                    if (workDays == null) workDays = "SunThu";
                    if (shiftType == null) shiftType = "2";

                    days = buildDays(workDays);
                    shifts = buildShifts(shiftType);

                    updateSummaryText(0, 0, 0);
                    refreshButtons();
                })
                .addOnFailureListener(e -> {
                    days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
                    shifts = Arrays.asList("Morning", "Evening");
                    updateSummaryText(0, 0, 0);
                    refreshButtons();
                });
    }
    // Build - Allowed only after the submission has closed and the deadline has passed
    private boolean isBuildAllowed() {
        long now = System.currentTimeMillis();
        boolean deadlinePassed = (deadlineMillis > 0) && now > deadlineMillis;
        boolean noDeadline = (deadlineMillis <= 0);
        return (!submissionOpen) && (deadlinePassed || noDeadline);
    }

    private void refreshButtons() {
        btnBuild.setEnabled(isBuildAllowed());

// // Publish is only allowed if we have already built and saved
        btnPublish.setEnabled(scheduleBuilt && !schedulePublished);
    }

    // =========================
// 2) Build
// =========================
    private void buildNow() {
        if (!isBuildAllowed()) {
            Toast.makeText(requireContext(), "Build not allowed now (submission open / deadline not passed)", Toast.LENGTH_SHORT).show();
            return;
        }

        tvSummary.setText("Loading employees & constraints...");
        btnBuild.setEnabled(false);
        btnPublish.setEnabled(false);

        final String finalWeekId = weekId;

        db.collection("users").get()
                .addOnSuccessListener(usersSnap -> {

                    List<EmployeeRec> employees = new ArrayList<>();

                    for (DocumentSnapshot u : usersSnap.getDocuments()) {
                        String role = u.getString("role");
                        if (!"employee".equalsIgnoreCase(role)) continue;

                        String uid = u.getId();
                        String fullName = u.getString("fullName");
                        if (TextUtils.isEmpty(fullName)) fullName = uid;

                        employees.add(new EmployeeRec(uid, fullName));
                    }

                    if (employees.isEmpty()) {
                        tvSummary.setText("No employees found.");
                        refreshButtons();
                        return;
                    }

                    loadAvailabilityAtIndex(finalWeekId, employees, 0);

                })
                .addOnFailureListener(e -> {
                    tvSummary.setText("Failed to load employees: " + e.getMessage());
                    refreshButtons();
                });
    }

    private void loadAvailabilityAtIndex(String finalWeekId, List<EmployeeRec> employees, int idx) {
        if (idx >= employees.size()) {
// build schedule
            Map<String, SlotAssignment> schedule = buildScheduleRoundRobin(employees);

// convert for manager display + for save
            lastScheduleDisplay = new LinkedHashMap<>();
            lastAssignmentsToSave = new LinkedHashMap<>();

            for (Map.Entry<String, SlotAssignment> e : schedule.entrySet()) {
                String slotKey = e.getKey();
                SlotAssignment sa = e.getValue();

                if (sa == null || sa.fullName == null || sa.fullName.trim().isEmpty()) {
                    lastScheduleDisplay.put(slotKey, "UNASSIGNED");
                } else {
                    lastScheduleDisplay.put(slotKey, sa.fullName);
                }

// save format for employee screen: map {fullName, uid}
                Map<String, Object> m = new HashMap<>();
                m.put("fullName", (sa == null) ? "" : sa.fullName);
                m.put("uid", (sa == null) ? "" : sa.uid);
                lastAssignmentsToSave.put(slotKey, m);
            }

            showScheduleInTable(lastScheduleDisplay);

            int totalEmp = employees.size();
            int submittedCount = countSubmitted(employees);
            int totalSlots = days.size() * shifts.size();
            int assignedSlots = countAssigned(lastScheduleDisplay);

            updateSummaryText(totalEmp, submittedCount, assignedSlots);

// save built schedule to Firestore (scheduleBuilt=true, schedulePublished=false)
            saveBuiltSchedule(finalWeekId, lastAssignmentsToSave);

            return;
        }

        EmployeeRec emp = employees.get(idx);

        db.collection("users")
                .document(emp.uid)
                .collection("availabilityByWeek")
                .document(finalWeekId)
                .get()
                .addOnSuccessListener(weekDoc -> {
                    if (weekDoc != null && weekDoc.exists()) {
                        Boolean sub = weekDoc.getBoolean("submitted");
                        emp.submitted = (sub != null && sub);
                        emp.availability = readAvailabilityMap(weekDoc.get("availability"));
                    } else {
                        emp.availability = new HashMap<>();
                    }
                    loadAvailabilityAtIndex(finalWeekId, employees, idx + 1);
                })
                .addOnFailureListener(e -> {
                    emp.availability = new HashMap<>();
                    loadAvailabilityAtIndex(finalWeekId, employees, idx + 1);
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> readAvailabilityMap(Object raw) {
        Map<String, Boolean> out = new HashMap<>();
        if (!(raw instanceof Map)) return out;

        Map<?, ?> m = (Map<?, ?>) raw;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() == null) continue;
            Object v = e.getValue();
            if (v instanceof Boolean) {
                out.put(String.valueOf(e.getKey()), (Boolean) v);
            }
        }
        return out;
    }

 // round-robin: selects an available worker for each slot
    private Map<String, SlotAssignment> buildScheduleRoundRobin(List<EmployeeRec> employees) {
        Map<String, SlotAssignment> result = new LinkedHashMap<>();

        List<String> slots = new ArrayList<>();
        for (String shift : shifts) {
            for (String day : days) {
                slots.add(day + "_" + shift);
            }
        }

        int n = employees.size();
        int start = 0;

        for (String slotKey : slots) {
            SlotAssignment chosen = new SlotAssignment("", "");

            for (int step = 0; step < n; step++) {
                int idx = (start + step) % n;
                EmployeeRec emp = employees.get(idx);

                Boolean can = emp.availability.get(slotKey);
                if (can != null && can) {
                    chosen = new SlotAssignment(emp.uid, emp.fullName);
                    start = (idx + 1) % n;
                    break;
                }
            }
// If not found – leave empty (will become UNASSIGNED in the display)
            result.put(slotKey, chosen);
        }

        return result;
    }

    private void showScheduleInTable(Map<String, String> scheduleDisplay) {
        List<String> cells = buildCells(days, shifts);
        int columns = 1 + days.size();
        rvTable.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        rvTable.setAdapter(new ScheduleResultAdapter(days, shifts, cells, scheduleDisplay));
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

    private int countSubmitted(List<EmployeeRec> employees) {
        int c = 0;
        for (EmployeeRec e : employees) if (e.submitted) c++;
        return c;
    }

    private int countAssigned(Map<String, String> scheduleDisplay) {
        int c = 0;
        for (String v : scheduleDisplay.values()) {
            if (v != null && !v.equalsIgnoreCase("UNASSIGNED") && !v.trim().isEmpty()) c++;
        }
        return c;
    }

    private void updateSummaryText(int employeesCount, int submittedCount, int assignedSlots) {
        int totalSlots = days.size() * shifts.size();
        int missingSlots = totalSlots - assignedSlots;

        String deadlineText = (deadlineMillis > 0)
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(deadlineMillis))
                : "none";

        String allowed = isBuildAllowed() ? "YES" : "NO";

        tvSummary.setText("Employees: " + employeesCount
                +" | Submitted: " + submittedCount + " | Missing: " + (employeesCount - submittedCount) + "\n" +
                        "Slots: " + totalSlots +
                        " | Assigned: " + assignedSlots +
                        " | Unassigned: " + missingSlots + "\n" +
                        "Deadline: " + deadlineText + "\n" +
                        "Build allowed now: " + allowed + " (weekId: '" + weekId + "')"
        );
    }

    // =========================
// 3) Save built schedule
// =========================
    private void saveBuiltSchedule(String finalWeekId, Map<String, Object> assignmentsToSave) {

        DocumentReference builtRef = db.collection("builtSchedules").document(finalWeekId);
        DocumentReference cfgRef = db.collection("scheduleConfig").document("currentWeek");

        Map<String, Object> builtDoc = new HashMap<>();
        builtDoc.put("weekId", finalWeekId);
        builtDoc.put("assignments", assignmentsToSave);
        builtDoc.put("createdAt", FieldValue.serverTimestamp());

// scheduleConfig update: built=true, published=false
        Map<String, Object> cfgUpdate = new HashMap<>();
        cfgUpdate.put("scheduleBuilt", true);
        cfgUpdate.put("schedulePublished", false);

        db.runBatch(batch -> {
            batch.set(builtRef, builtDoc);
            batch.set(cfgRef, cfgUpdate, SetOptions.merge());
        }).addOnSuccessListener(unused -> {
            scheduleBuilt = true;
            schedulePublished = false;
            Toast.makeText(requireContext(), "Built schedule saved ✅", Toast.LENGTH_SHORT).show();
            refreshButtons(); // עכשיו Publish יידלק
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(), "Save built schedule failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            refreshButtons();
        });
    }

    // =========================
// 4) Publish
// =========================
    private void publishNow() {
        if (!scheduleBuilt) {
            Toast.makeText(requireContext(), "Build first, then publish.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublish.setEnabled(false);

        db.collection("scheduleConfig").document("currentWeek")
                .set(new HashMap<String, Object>() {{
                    put("schedulePublished", true);
                }}, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    schedulePublished = true;
                    Toast.makeText(requireContext(), "Schedule published ✅", Toast.LENGTH_SHORT).show();
                    refreshButtons();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Publish failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    refreshButtons();
                });
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

    // model holders
    static class EmployeeRec {
        String uid;
        String fullName;
        boolean submitted = false;
        Map<String, Boolean> availability = new HashMap<>();

        EmployeeRec(String uid, String fullName) {
            this.uid = uid;
            this.fullName = fullName;
        }
    }

    static class SlotAssignment {
        String uid;
        String fullName;
        SlotAssignment(String uid, String fullName) {
            this.uid = uid;
            this.fullName = fullName;
        }
    }
}