package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        Button btnBack   = view.findViewById(R.id.btnBack);

        // ✅ SUBMIT: רק הודעה, לא חוזר אחורה
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                Toast.makeText(requireContext(),
                        "ההגשה התקבלה ✅",
                        Toast.LENGTH_SHORT).show();
                // לא עושים navigateUp()
            });
        }

        // ✅ BACK: חוזר למסך הקודם (המסך ביניים)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                NavHostFragment.findNavController(this).navigateUp();
            });
        }

        // Load shift config from Firestore
        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(cfg -> {
                    String workDays = cfg.getString("workDays");
                    String shiftType = cfg.getString("shiftType");
                    Boolean canSubmit = cfg.getBoolean("canSubmitConstraints");

                    if (workDays == null) workDays = "SunThu";
                    if (shiftType == null) shiftType = "2";
                    boolean canEdit = canSubmit != null && canSubmit;

                    List<String> days = buildDays(workDays);
                    List<String> shifts = buildShifts(shiftType);

                    loadUserAvailabilityAndSetup(rv, days, shifts, canEdit);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load shift settings", Toast.LENGTH_SHORT).show();
                    List<String> days = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu");
                    List<String> shifts = Arrays.asList("Morning", "Evening");
                    loadUserAvailabilityAndSetup(rv, days, shifts, true);
                });
    }

    private void loadUserAvailabilityAndSetup(RecyclerView rv,
                                              List<String> days,
                                              List<String> shifts,
                                              boolean canEdit) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            setupGrid(rv, days, shifts, new HashMap<>(), false, null);
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid)
                .get()
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

                    setupGrid(rv, days, shifts, availability, canEdit, uid);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load availability", Toast.LENGTH_SHORT).show();
                    setupGrid(rv, days, shifts, new HashMap<>(), canEdit, uid);
                });
    }

    private void setupGrid(RecyclerView rv,
                           List<String> days,
                           List<String> shifts,
                           Map<String, Boolean> availability,
                           boolean canEdit,
                           @Nullable String uid) {

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

                    db.collection("users").document(uid)
                            .update("availability." + key, newValue)
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Save failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }
        );

        rv.setAdapter(adapter);

        if (!canEdit) {
            Toast.makeText(requireContext(), "Submission is CLOSED (manager)", Toast.LENGTH_SHORT).show();
        }
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
