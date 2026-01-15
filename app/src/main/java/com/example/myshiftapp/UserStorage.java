package com.example.myshiftapp;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStorage {

    // SharedPreferences
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_SEEDED = "seeded";

    // User keys (by email)
    private static final String KEY_USER = "user_";
    private static final String KEY_PASS = "_pass";
    private static final String KEY_ROLE = "_role";
    private static final String KEY_FIRST_LOGIN = "_first_login";
    private static final String KEY_NAME = "_name"; // optional now, useful later

    // Call once at app start (MainActivity)
    public static void seedUsersIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean seeded = prefs.getBoolean(KEY_SEEDED, false);
        if (seeded) return;

        SharedPreferences.Editor editor = prefs.edit();

        // Demo users (Option A: temp password + firstLogin=true)
        // Manager
        putUser(editor,
                "manager@myshift.com",
                "Admin1",           // must match password rules
                "manager",
                "Dana Manager",
                true
        );

        // Employee
        putUser(editor,
                "employee@myshift.com",
                "Work1",
                "employee",
                "Noam Employee",
                true
        );

        editor.putBoolean(KEY_SEEDED, true);
        editor.apply();
    }

    private static void putUser(SharedPreferences.Editor editor,
                                String email, String pass, String role, String name, boolean firstLogin) {
        editor.putString(KEY_USER + email + KEY_PASS, pass);
        editor.putString(KEY_USER + email + KEY_ROLE, role);
        editor.putString(KEY_USER + email + KEY_NAME, name);
        editor.putBoolean(KEY_USER + email + KEY_FIRST_LOGIN, firstLogin);
    }

    public static boolean isUserKnown(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_USER + email + KEY_PASS);
    }

    public static boolean checkPassword(Context context, String email, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPass = prefs.getString(KEY_USER + email + KEY_PASS, null);
        return savedPass != null && savedPass.equals(password);
    }

    public static String getRole(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER + email + KEY_ROLE, "employee");
    }

    public static boolean isFirstLogin(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_USER + email + KEY_FIRST_LOGIN, true);
    }

    public static void setPasswordAndMarkFirstLoginDone(Context context, String email, String newPass) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER + email + KEY_PASS, newPass)
                .putBoolean(KEY_USER + email + KEY_FIRST_LOGIN, false)
                .apply();
    }

    // optional helper
    /*
    public static String getName(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER + email + KEY_NAME, "");
    }*/
}
