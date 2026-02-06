package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftScheduleFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ShiftScheduleFragment() {
        super(R.layout.fragment_shift_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        RecyclerView rv = view.findViewById(R.id.rvSchedule);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);
        Button btnBack = view.findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        }

// 1) Load scheduleConfig/currentWeek (open/deadline/weekId)
        db.collection("scheduleConfig").document("currentWeek")
                .get()
                .addOnSuccessListener(cfg -> {

                    Boolean submissionOpen = cfg.getBoolean("submissionOpen");
                    Long deadlineMillis = cfg.getLong("submissionDeadlineMillis");

// NEW: currentWeekId (final, so we can use inside lambdas)
                    String weekIdRaw = cfg.getString("currentWeekId");
                    final String weekId = (weekIdRaw == null || weekIdRaw.trim().isEmpty())
                            ? "currentWeek"
                            : weekIdRaw.trim();

                    boolean isOpen = submissionOpen != null && submissionOpen;

                    long now = System.currentTimeMillis();
                    boolean hasDeadline = (deadlineMillis != null && deadlineMillis > 0);
                    boolean beforeDeadline = !hasDeadline || now <= deadlineMillis;

                    final boolean canEdit = isOpen && beforeDeadline;

// 2) Load shift config (days / shift type)
                    db.collection("settings").document("shift_config")
                            .get()
                            .addOnSuccessListener(shiftCfg -> {

                                String workDays = shiftCfg.getString("workDays");
                                String shiftType = shiftCfg.getString("shiftType");

                                if (workDays == null) workDays = "SunThu";
                                if (shiftType == null) shiftType = "2";

                                List<String> days = buildDays(workDays);
                                List<String> shifts = buildShifts(shiftType);

                                loadUserAvailabilityAndSetup(rv, days, shifts, canEdit, btnSubmit, weekId);

                                if (!canEdit) {
                                    Toast.makeText(requireContext(),
                                            "Submission is CLOSED (deadline passed / manager closed)",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(),
                                        "Failed to load shift settings: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();

                                List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
                                List<String> shifts = Arrays.asList("Morning", "Evening");

                                loadUserAvailabilityAndSetup(rv, days, shifts, canEdit, btnSubmit, weekId);
                            });
                })
                .addOnFailureListener(e -> {
// If no scheduleConfig/currentWeek – default OPEN so the app continues
                    Toast.makeText(requireContext(),
                            "scheduleConfig/currentWeek not found - using default OPEN",
                            Toast.LENGTH_LONG).show();

                    List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
                    List<String> shifts = Arrays.asList("Morning", "Evening");

                    loadUserAvailabilityAndSetup(rv, days, shifts, true, btnSubmit, "currentWeek");
                });
    }

    private void loadUserAvailabilityAndSetup(RecyclerView rv,
                                              List<String> days,
                                              List<String> shifts,
                                              boolean canEdit,
                                              @Nullable Button btnSubmit,
                                              @NonNull String weekId) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            setupGrid(rv, days, shifts, new HashMap<>(), false, null, weekId);
            if (btnSubmit != null) btnSubmit.setEnabled(false);
            return;
        }

        final String uid = user.getUid();

// Firestore doc for this user's current week submission
        final DocumentReference weekDocRef = db.collection("users")
                .document(uid)
                .collection("availabilityByWeek")
                .document(weekId);

// Submit button: mark submitted + submittedAt on the week doc
        if (btnSubmit != null) {
            btnSubmit.setEnabled(canEdit);
            btnSubmit.setOnClickListener(v -> {
                if (!canEdit) {
                    Toast.makeText(requireContext(), "Submission is closed", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("submitted", true);
                updates.put("submittedAt", FieldValue.serverTimestamp());
                updates.put("exempt", false);
                updates.put("exemptReason", "");

// Use set(..., merge) pattern by updating + creating if missing:
                weekDocRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(unused ->
                                Toast.makeText(requireContext(), "Submitted ✅", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Submit failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        }

// Load existing availability from users/{uid}/availabilityByWeek/{weekId}
        weekDocRef.get()
                .addOnSuccessListener(doc -> {
                    Map<String, Boolean> availability = new HashMap<>();

                    Object raw = doc.get("availability");
                    if (raw instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) raw;
                        for (Map.Entry<?, ?> entry : m.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() instanceof Boolean) {
                                availability.put(String.valueOf(entry.getKey()), (Boolean) entry.getValue());
                            }
                        }
                    }

                    setupGrid(rv, days, shifts, availability, canEdit, uid, weekId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupGrid(rv, days, shifts, new HashMap<>(), canEdit, uid, weekId);
                });
    }

    private void setupGrid(RecyclerView rv,
                           List<String> days,
                           List<String> shifts,
                           Map<String, Boolean> availability,
                           boolean canEdit,
                           @Nullable String uid,
                           @NonNull String weekId) {

        List<String> cells = buildCells(days, shifts);
        int columns = 1 + days.size();

        rv.setLayoutManager(new GridLayoutManager(requireContext(), columns));

        ScheduleAdapter adapter = new ScheduleAdapter(
                requireContext(),
                days,
                shifts,
                cells,
                availability,
                canEdit,
                (key, newValue) -> {
                    Log.d("GRID", "toggle " + key + " -> " + newValue);

                    if (uid == null) return;

// Update nested field: availability.Sun_Morning = true/false
                    db.collection("users")
                            .document(uid)
                            .collection("availabilityByWeek")
                            .document(weekId)
                            .set(new HashMap<String, Object>() {{
                                put("availability", new HashMap<String, Object>() {{
                                    put(key, newValue);
                                }});
                            }}, com.google.firebase.firestore.SetOptions.merge())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Save failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }
        );

        rv.setAdapter(adapter);
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