package com.example.myshiftapp;

import java.util.List;
import java.util.Locale;

public class PayslipCalculator {

    public static class Result {
        public long totalMinutes;
        public double basePay; // minutes * hourlyRate (without multipliers)
        public double extrasPay; // extra amount due to multipliers (night, etc.)
        public double grossPay; // basePay + extrasPay

        public Result(long totalMinutes, double basePay, double extrasPay) {
            this.totalMinutes = totalMinutes;
            this.basePay = basePay;
            this.extrasPay = extrasPay;
            this.grossPay = basePay + extrasPay;
        }
    }

    /**
     * Calculates payslip for approved records.
     * We assume: Morning/Evening multiplier = 1.0, Night multiplier = 1.25 (example).
     */
    public static Result calculate(List<AttendanceRecord> approvedRecords, double hourlyRate) {
        long totalMinutes = 0;

// Base pay = totalMinutes * hourlyRate (without multipliers)
// Extras pay = sum( minutes * hourlyRate * (multiplier - 1) )
        double basePay = 0.0;
        double extrasPay = 0.0;

        if (approvedRecords == null || approvedRecords.isEmpty()) {
            return new Result(0, 0.0, 0.0);
        }

        for (AttendanceRecord r : approvedRecords) {
            if (r == null) continue;

            long mins = Math.max(0, r.durationMinutes);
            totalMinutes += mins;

            double hours = mins / 60.0;
            double mult = getMultiplier(r.shiftType);

// base (no multiplier)
            basePay += hours * hourlyRate;

// extras due to multiplier
            extrasPay += hours * hourlyRate * (mult - 1.0);
        }

        return new Result(totalMinutes, round2(basePay), round2(extrasPay));
    }

    /**
     * Shift multipliers based on Spinner values:
     * Morning, Evening, Night.
     */
    public static double getMultiplier(String shiftType) {
        if (shiftType == null) return 1.0;

        String s = shiftType.trim().toLowerCase(Locale.ROOT);
        if (s.equals("night")) return 1.25;

// Morning / Evening
        return 1.0;
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}
