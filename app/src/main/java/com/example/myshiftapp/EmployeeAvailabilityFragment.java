package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmployeeAvailabilityFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvStatus;

    private Button btnSave, btnBack;

    // Night checkboxes (we will hide them if shiftType="2")
    private CheckBox cbSunNight, cbMonNight, cbTueNight, cbWedNight, cbThuNight, cbFriNight, cbSatNight;

    // Day title/rows to hide if not in workDays
    private View tvFri, rowFri, tvSat, rowSat;

    // All checkboxes (Morning/Evening/Night) – we will read their values on save
    private CheckBox cbSunMorning, cbSunEvening;
    private CheckBox cbMonMorning, cbMonEvening;
    private CheckBox cbTueMorning, cbTueEvening;
    private CheckBox cbWedMorning, cbWedEvening;
    private CheckBox cbThuMorning, cbThuEvening;
    private CheckBox cbFriMorning, cbFriEvening;
    private CheckBox cbSatMorning, cbSatEvening;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_availability, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvStatus = view.findViewById(R.id.tvStatus);
        btnSave = view.findViewById(R.id.btnSave);
        btnBack = view.findViewById(R.id.btnBack);

        // Bind checkboxes
        cbSunMorning = view.findViewById(R.id.cbSunMorning);
        cbSunEvening = view.findViewById(R.id.cbSunEvening);
        cbSunNight = view.findViewById(R.id.cbSunNight);

        cbMonMorning = view.findViewById(R.id.cbMonMorning);
        cbMonEvening = view.findViewById(R.id.cbMonEvening);
        cbMonNight = view.findViewById(R.id.cbMonNight);

        cbTueMorning = view.findViewById(R.id.cbTueMorning);
        cbTueEvening = view.findViewById(R.id.cbTueEvening);
        cbTueNight = view.findViewById(R.id.cbTueNight);

        cbWedMorning = view.findViewById(R.id.cbWedMorning);
        cbWedEvening = view.findViewById(R.id.cbWedEvening);
        cbWedNight = view.findViewById(R.id.cbWedNight);

        cbThuMorning = view.findViewById(R.id.cbThuMorning);
        cbThuEvening = view.findViewById(R.id.cbThuEvening);
        cbThuNight = view.findViewById(R.id.cbThuNight);

        cbFriMorning = view.findViewById(R.id.cbFriMorning);
        cbFriEvening = view.findViewById(R.id.cbFriEvening);
        cbFriNight = view.findViewById(R.id.cbFriNight);

        cbSatMorning = view.findViewById(R.id.cbSatMorning);
        cbSatEvening = view.findViewById(R.id.cbSatEvening);
        cbSatNight = view.findViewById(R.id.cbSatNight);

        // Views for Fri/Sat to hide when workDays=SunThu
        tvFri = view.findViewById(R.id.tvFri);
        rowFri = view.findViewById(R.id.rowFri);
        tvSat = view.findViewById(R.id.tvSat);
        rowSat = view.findViewById(R.id.rowSat);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnSave.setOnClickListener(v -> saveAvailability());

        // Load manager config and adjust UI
        loadShiftConfigAndAdjustUI();

        return view;
    }

    private void loadShiftConfigAndAdjustUI() {
        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        tvStatus.setText("No settings found. Ask manager to configure.");
                        btnSave.setEnabled(false);
                        return;
                    }

                    String shiftType = doc.getString("shiftType"); // "2" / "3"
                    String workDays = doc.getString("workDays");   // "SunThu" / "SunFri" / "SunSat"

                    if (shiftType == null) shiftType = "2";
                    if (workDays == null) workDays = "SunThu";

                    // 1) Hide night if shiftType=2
                    if ("2".equals(shiftType)) {
                        hideNight();
                    }

                    // 2) Hide Fri/Sat based on workDays
                    if ("SunThu".equals(workDays)) {
                        hideFridayAndSaturday();
                    } else if ("SunFri".equals(workDays)) {
                        hideSaturdayOnly();
                    } // SunSat -> show all

                    tvStatus.setText("Loaded settings ✅");
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Failed to load settings: " + e.getMessage());
                    btnSave.setEnabled(false);
                });
    }

    private void hideNight() {
        cbSunNight.setVisibility(View.GONE);
        cbMonNight.setVisibility(View.GONE);
        cbTueNight.setVisibility(View.GONE);
        cbWedNight.setVisibility(View.GONE);
        cbThuNight.setVisibility(View.GONE);
        cbFriNight.setVisibility(View.GONE);
        cbSatNight.setVisibility(View.GONE);
    }

    private void hideFridayAndSaturday() {
        tvFri.setVisibility(View.GONE);
        rowFri.setVisibility(View.GONE);
        tvSat.setVisibility(View.GONE);
        rowSat.setVisibility(View.GONE);
    }

    private void hideSaturdayOnly() {
        tvSat.setVisibility(View.GONE);
        rowSat.setVisibility(View.GONE);
    }

    private void saveAvailability() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        // Build a structured map: days -> shifts
        Map<String, Object> days = new HashMap<>();

        days.put("Sun", makeDay(cbSunMorning, cbSunEvening, cbSunNight));
        days.put("Mon", makeDay(cbMonMorning, cbMonEvening, cbMonNight));
        days.put("Tue", makeDay(cbTueMorning, cbTueEvening, cbTueNight));
        days.put("Wed", makeDay(cbWedMorning, cbWedEvening, cbWedNight));
        days.put("Thu", makeDay(cbThuMorning, cbThuEvening, cbThuNight));
        days.put("Fri", makeDay(cbFriMorning, cbFriEvening, cbFriNight));
        days.put("Sat", makeDay(cbSatMorning, cbSatEvening, cbSatNight));

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("days", days);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("availabilities").document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    tvStatus.setText("Saved ✅");
                    Toast.makeText(requireContext(), "Availability saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Save failed: " + e.getMessage());
                    Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Object> makeDay(CheckBox morning, CheckBox evening, CheckBox night) {
        Map<String, Object> m = new HashMap<>();
        m.put("morning", morning.isChecked());
        m.put("evening", evening.isChecked());
        // אם Night מוסתר (shiftType=2) הוא עדיין קיים אבל זה בסדר שיהיה false
        m.put("night", night.isChecked());
        return m;
    }
}
