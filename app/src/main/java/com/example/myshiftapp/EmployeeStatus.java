package com.example.myshiftapp;

public class EmployeeStatus {
    public String uid;
    public String fullName;
    public boolean submitted;
    public String submittedAtText;

    public EmployeeStatus(String uid, String fullName, boolean submitted, String submittedAtText) {
        this.uid = uid;
        this.fullName = fullName;
        this.submitted = submitted;
        this.submittedAtText = submittedAtText;
    }
}
