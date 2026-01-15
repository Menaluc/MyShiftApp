package com.example.myshiftapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class UserStorage {

    // SharedPreferences file name
    private static final String PREFS_NAME = "UserPrefs";

    // Keys
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ROLE = "role";

    // Save current logged-in user (after successful Firebase login + Firestore fetch)
    public static void saveCurrentUser(Context context,
                                       String uid,
                                       String email,
                                       String fullName,
                                       String role) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_EMAIL, email)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_ROLE, role)
                .apply();
    }

    // Clear current user on logout
    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    // Helpers (getters)
    @Nullable
    public static String getUid(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_UID, null);
    }

    @Nullable
    public static String getEmail(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, null);
    }

    public static String getFullName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FULL_NAME, "");
    }

    public static String getRole(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ROLE, "");
    }

    public static boolean isLoggedIn(Context context) {
        return getUid(context) != null;
    }

    public static boolean isManager(Context context) {
        return "manager".equalsIgnoreCase(getRole(context));
    }
}
