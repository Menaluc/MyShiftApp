package com.example.myshiftapp;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStorage {

    // SharedPreferences
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_SEEDED = "seeded";

    // Users data keys
    private static final String KEY_USER_ID = "user_id_";
    private static final String KEY_PASS = "_pass";
    private static final String KEY_ROLE = "_role";

    // First login flag (for each user)
    private static final String KEY_FIRST_LOGIN_DONE = "_first_login_done";

    // Call this once at app start (MainActivity)
    public static void seedUsersIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean seeded = prefs.getBoolean(KEY_SEEDED, false);
        if (seeded) return;

        SharedPreferences.Editor editor = prefs.edit();

        // Example users (change to your real ones later)
        // Manager
        editor.putString(KEY_USER_ID + "123456789" + KEY_PASS, "Admin1");   // must match your password rules
        editor.putString(KEY_USER_ID + "123456789" + KEY_ROLE, "manager");
        editor.putBoolean(KEY_USER_ID + "123456789" + KEY_FIRST_LOGIN_DONE, false);

        // Employee
        editor.putString(KEY_USER_ID + "987654321" + KEY_PASS, "Work1");
        editor.putString(KEY_USER_ID + "987654321" + KEY_ROLE, "employee");
        editor.putBoolean(KEY_USER_ID + "987654321" + KEY_FIRST_LOGIN_DONE, false);

        editor.putBoolean(KEY_SEEDED, true);
        editor.apply();
    }

    // ===== Helpers that match your LoginFragment names =====

    // Check if user exists in storage
    public static boolean isUserKnown(Context context, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPass = prefs.getString(KEY_USER_ID + userId + KEY_PASS, null);
        return savedPass != null;
    }

    // Check password equals stored password
    public static boolean checkPassword(Context context, String userId, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPass = prefs.getString(KEY_USER_ID + userId + KEY_PASS, null);
        return savedPass != null && savedPass.equals(password);
    }

    // First login means: first_login_done == false
    public static boolean isFirstLogin(Context context, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return !prefs.getBoolean(KEY_USER_ID + userId + KEY_FIRST_LOGIN_DONE, false);
    }

    public static String getRole(Context context, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID + userId + KEY_ROLE, "employee");
    }

    /*
    public static void updatePassword(Context context, String userId, String newPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_ID + userId + KEY_PASS, newPassword).apply();
    }*/

    // Called from ChangePasswordFragment: save new password + mark first login done
    public static void setPasswordAndMarkFirstLoginDone(Context context, String userId, String newPass) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_ID + userId + KEY_PASS, newPass)
                .putBoolean(KEY_USER_ID + userId + KEY_FIRST_LOGIN_DONE, true)
                .apply();
    }

    //Backward compatibility if you still use this somewhere
    /*
    public static boolean checkLogin(Context context, String userId, String password) {
        return checkPassword(context, userId, password);
    }*/
}
