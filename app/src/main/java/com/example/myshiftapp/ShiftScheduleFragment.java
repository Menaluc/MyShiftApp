package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftScheduleFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView rvSchedule;

    public ShiftScheduleFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_shift_schedule, container, false);

        rvSchedule = view.findViewById(R.id.rvSchedule);
        db = FirebaseFirestore.getInstance();

        loadConfigAndBuildTable();

        return view;
    }

    private void loadConfigAndBuildTable() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();

        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(requireContext(), "shift_config not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String workDays = doc.getString("workDays");           // SunThu / SunFri / SunSat
                    String shiftType = doc.getString("shiftType");         // "2" / "3"
                    Boolean canSubmitObj = doc.getBoolean("switchCanConstraints"); // true/false

                    boolean canEdit = (canSubmitObj != null && canSubmitObj);

                    List<String> days = getDaysByWorkDays(workDays);
                    List<String> shifts = getShiftsByType(shiftType);

                    int columns = 1 + days.size();
                    rvSchedule.setLayoutManager(new GridLayoutManager(requireContext(), columns));

                    // Load existing availability from Firestore
                    loadAvailabilityAndSetAdapter(uid, days, shifts, canEdit);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadAvailabilityAndSetAdapter(String uid, List<String> days, List<String> shifts, boolean canEdit) {
        db.collection("constraints").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Map<String, Boolean> availability = new HashMap<>();

                    if (doc != null && doc.exists()) {
                        Object obj = doc.get("availability");
                        if (obj instanceof Map) {
                            Map<?, ?> raw = (Map<?, ?>) obj;
                            for (Map.Entry<?, ?> e : raw.entrySet()) {
                                if (e.getKey() instanceof String && e.getValue() instanceof Boolean) {
                                    availability.put((String) e.getKey(), (Boolean) e.getValue());
                                }
                            }
                        }
                    }

                    List<String> cells = buildCells(days, shifts);

                    ScheduleAdapter adapter = new ScheduleAdapter(
                            requireContext(),
                            days,
                            shifts,
                            cells,
                            availability,
                            canEdit,
                            (key, newValue) -> saveCell(uid, key, newValue)
                    );

                    rvSchedule.setAdapter(adapter);

                    if (!canEdit) {
                        Toast.makeText(requireContext(), "Submission is currently closed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveCell(String uid, String key, Boolean value) {
        // Save single cell: availability.<key> = true/false
        Map<String, Object> update = new HashMap<>();
        update.put("availability." + key, value);

        db.collection("constraints").document(uid)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private List<String> getDaysByWorkDays(String workDays) {
        if ("SunSat".equalsIgnoreCase(workDays)) {
            return Arrays.asList("Sun","Mon","Tue","Wed","Thu","Fri","Sat");
        } else if ("SunFri".equalsIgnoreCase(workDays)) {
            return Arrays.asList("Sun","Mon","Tue","Wed","Thu","Fri");
        } else {
            return Arrays.asList("Sun","Mon","Tue","Wed","Thu"); // SunThu default
        }
    }

    private List<String> getShiftsByType(String shiftType) {
        if ("3".equals(shiftType)) return Arrays.asList("Morning", "Evening", "Night");
        return Arrays.asList("Morning", "Evening");
    }

    // Build table: first row headers (days), first column shift names
    private List<String> buildCells(List<String> days, List<String> shifts) {
        List<String> cells = new ArrayList<>();

        // Header row
        cells.add(""); // top-left empty
        cells.addAll(days);

        // Shift rows
        for (String shift : shifts) {
            cells.add(shift);
            for (int i = 0; i < days.size(); i++) {
                cells.add(""); // content controlled by adapter (✓ / X)
            }
        }

        return cells;
    }
}
