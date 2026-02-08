package com.example.myshiftapp;

// Simple model class for one attendance record
public class AttendanceRecord{
    public String id;
    public String shiftType;
    public long startTimeMillis;
    public long endTimeMillis;
    public long durationMinutes;
    public String status; // PENDING / APPROVED / REJECTED

    public AttendanceRecord() { }

    public AttendanceRecord(String id, String shiftType, long startTimeMillis, long endTimeMillis,
                            long durationMinutes, String status) {
        this.id = id;
        this.shiftType = shiftType;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }
}