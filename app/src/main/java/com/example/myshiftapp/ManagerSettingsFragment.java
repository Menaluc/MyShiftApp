package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ManagerSettingsFragment extends Fragment {

    private FirebaseFirestore db;

    private SwitchMaterial switchCanSubmit;

    private RadioGroup rgShiftTypes;
    private RadioButton rb2Shifts, rb3Shifts;

    private EditText etMorningStart, etMorningEnd;
    private EditText etEveningStart, etEveningEnd;
    private EditText etNightStart, etNightEnd;

    private EditText etEmployeesPerShift;

    private RadioGroup rgWorkDays;
    private RadioButton rbSunThu, rbSunFri, rbSunSat;

    // ✅ Deadline field (picker-based)
    private EditText etSubmitCloseAt;
    private Long submitCloseAtMillis = null;

    private Button btnSaveSettings, btnBack;
    private TextView tvStatus;

    private static final String SETTINGS_COLLECTION = "settings";
    private static final String SETTINGS_DOC = "shift_config";

    public ManagerSettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manager_settings, container, false);

        db = FirebaseFirestore.getInstance();

        switchCanSubmit = view.findViewById(R.id.switchCanSubmit);

        rgShiftTypes = view.findViewById(R.id.rgShiftTypes);
        rb2Shifts = view.findViewById(R.id.rb2Shifts);
        rb3Shifts = view.findViewById(R.id.rb3Shifts);

        etMorningStart = view.findViewById(R.id.etMorningStart);
        etMorningEnd = view.findViewById(R.id.etMorningEnd);

        etEveningStart = view.findViewById(R.id.etEveningStart);
        etEveningEnd = view.findViewById(R.id.etEveningEnd);

        etNightStart = view.findViewById(R.id.etNightStart);
        etNightEnd = view.findViewById(R.id.etNightEnd);

        etEmployeesPerShift = view.findViewById(R.id.etEmployeesPerShift);

        rgWorkDays = view.findViewById(R.id.rgWorkDays);
        rbSunThu = view.findViewById(R.id.rbSunThu);
        rbSunFri = view.findViewById(R.id.rbSunFri);
        rbSunSat = view.findViewById(R.id.rbSunSat);

        etSubmitCloseAt = view.findViewById(R.id.etSubmitCloseAt);

        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);
        btnBack = view.findViewById(R.id.btnBack);
        tvStatus = view.findViewById(R.id.tvStatus);

        // Load settings
        loadSettingsFromFirestore();

        // When shift type changes, enable/disable night fields
        rgShiftTypes.setOnCheckedChangeListener((group, checkedId) -> updateNightFieldsVisibility());

        // ✅ Deadline pickers
        etSubmitCloseAt.setOnClickListener(v -> openDeadlinePickers());

        // Save
        btnSaveSettings.setOnClickListener(v -> saveSettingsToFirestore());

        // Back
        btnBack.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack());

        return view;
    }

    private void loadSettingsFromFirestore() {
        db.collection(SETTINGS_COLLECTION).document(SETTINGS_DOC)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        tvStatus.setText("No settings found yet. Please set and save.");
                        updateNightFieldsVisibility();
                        return;
                    }

                    Boolean canSubmit = doc.getBoolean("canSubmitConstraints");
                    String shiftType = doc.getString("shiftType");
                    String workDays = doc.getString("workDays");

                    switchCanSubmit.setChecked(canSubmit != null && canSubmit);

                    if ("3".equals(shiftType)) rb3Shifts.setChecked(true);
                    else rb2Shifts.setChecked(true);

                    etMorningStart.setText(defaultStr(doc.getString("morningStart")));
                    etMorningEnd.setText(defaultStr(doc.getString("morningEnd")));
                    etEveningStart.setText(defaultStr(doc.getString("eveningStart")));
                    etEveningEnd.setText(defaultStr(doc.getString("eveningEnd")));
                    etNightStart.setText(defaultStr(doc.getString("nightStart")));
                    etNightEnd.setText(defaultStr(doc.getString("nightEnd")));

                    Long employees = doc.getLong("employeesPerShift");
                    etEmployeesPerShift.setText(employees == null ? "" : String.valueOf(employees));

                    if ("SunFri".equals(workDays)) rbSunFri.setChecked(true);
                    else if ("SunSat".equals(workDays)) rbSunSat.setChecked(true);
                    else rbSunThu.setChecked(true);

                    // ✅ Load deadline millis
                    submitCloseAtMillis = doc.getLong("submitCloseAtMillis");
                    if (submitCloseAtMillis != null) {
                        etSubmitCloseAt.setText(formatMillis(submitCloseAtMillis));
                    } else {
                        etSubmitCloseAt.setText("");
                    }

                    tvStatus.setText("Settings loaded.");
                    updateNightFieldsVisibility();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Failed to load settings: " + e.getMessage());
                    updateNightFieldsVisibility();
                });
    }

    private void saveSettingsToFirestore() {
        boolean canSubmit = switchCanSubmit.isChecked();
        String shiftType = rb3Shifts.isChecked() ? "3" : "2";

        String morningStart = safeText(etMorningStart);
        String morningEnd = safeText(etMorningEnd);
        String eveningStart = safeText(etEveningStart);
        String eveningEnd = safeText(etEveningEnd);

        String nightStart = safeText(etNightStart);
        String nightEnd = safeText(etNightEnd);

        String employeesStr = safeText(etEmployeesPerShift);

        String workDays;
        if (rbSunFri.isChecked()) workDays = "SunFri";
        else if (rbSunSat.isChecked()) workDays = "SunSat";
        else workDays = "SunThu";

        // ✅ If submissions are enabled, require a deadline
        if (canSubmit && submitCloseAtMillis == null) {
            Toast.makeText(requireContext(), "Please choose a submission deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation times
        if (!isValidTime(morningStart) || !isValidTime(morningEnd) ||
                !isValidTime(eveningStart) || !isValidTime(eveningEnd)) {
            Toast.makeText(requireContext(), "Please enter valid times (HH:MM)", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("3".equals(shiftType)) {
            if (!isValidTime(nightStart) || !isValidTime(nightEnd)) {
                Toast.makeText(requireContext(), "Please enter valid night shift times (HH:MM)", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            nightStart = "";
            nightEnd = "";
        }

        if (TextUtils.isEmpty(employeesStr)) {
            Toast.makeText(requireContext(), "Please enter employees per shift", Toast.LENGTH_SHORT).show();
            return;
        }

        int employeesPerShift;
        try {
            employeesPerShift = Integer.parseInt(employeesStr);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Employees per shift must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (employeesPerShift <= 0) {
            Toast.makeText(requireContext(), "Employees per shift must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("canSubmitConstraints", canSubmit);
        data.put("shiftType", shiftType);

        data.put("morningStart", morningStart);
        data.put("morningEnd", morningEnd);
        data.put("eveningStart", eveningStart);
        data.put("eveningEnd", eveningEnd);

        data.put("nightStart", nightStart);
        data.put("nightEnd", nightEnd);

        data.put("employeesPerShift", employeesPerShift);
        data.put("workDays", workDays);

        // ✅ Save deadline (millis)
        data.put("submitCloseAtMillis", submitCloseAtMillis);

        db.collection(SETTINGS_COLLECTION).document(SETTINGS_DOC)
                .set(data)
                .addOnSuccessListener(unused -> {
                    tvStatus.setText("Settings saved successfully");
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Save failed: " + e.getMessage());
                    Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openDeadlinePickers() {
        // 1) pick date
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select deadline date")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // build calendar from selected date
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);

            // 2) pick time
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTitleText("Select deadline time")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                cal.set(Calendar.MINUTE, timePicker.getMinute());
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                submitCloseAtMillis = cal.getTimeInMillis();
                etSubmitCloseAt.setText(formatMillis(submitCloseAtMillis));
            });

            timePicker.show(getParentFragmentManager(), "timePicker");
        });

        datePicker.show(getParentFragmentManager(), "datePicker");
    }

    private void updateNightFieldsVisibility() {
        boolean isThreeShifts = rb3Shifts.isChecked();

        etNightStart.setEnabled(isThreeShifts);
        etNightEnd.setEnabled(isThreeShifts);

        View root = getView();
        if (root != null) {
            TextView tvNightTitle = root.findViewById(R.id.tvNightTitle);
            if (tvNightTitle != null) tvNightTitle.setEnabled(isThreeShifts);
        }
    }

    private String safeText(EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private String defaultStr(String s) {
        return (s == null) ? "" : s;
    }

    private String formatMillis(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    // Very simple HH:MM validation (00-23 : 00-59)
    private boolean isValidTime(String t) {
        if (TextUtils.isEmpty(t)) return false;
        if (!t.matches("^\\d{2}:\\d{2}$")) return false;

        int hh = Integer.parseInt(t.substring(0, 2));
        int mm = Integer.parseInt(t.substring(3, 5));

        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }
}
