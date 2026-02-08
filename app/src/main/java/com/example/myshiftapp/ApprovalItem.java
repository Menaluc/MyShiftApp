package com.example.myshiftapp;

public class ApprovalItem {
    public String uid; // employee uid
    public String recordId; // attendance record doc id
    public String employeeName; // display name (or uid if unknown)

    public String shiftType;
    public long startTimeMillis;
    public long endTimeMillis;
    public long durationMinutes;
    public String status; // PENDING / APPROVED / REJECTED

    public ApprovalItem() { }

    public ApprovalItem(String uid,
                        String recordId,
                        String employeeName,
                        String shiftType,
                        long startTimeMillis,
                        long endTimeMillis,
                        long durationMinutes,
                        String status) {
        this.uid = uid;
        this.recordId = recordId;
        this.employeeName = employeeName;
        this.shiftType = shiftType;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }
}