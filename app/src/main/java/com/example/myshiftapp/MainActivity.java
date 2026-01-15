package com.example.myshiftapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) return;

        // 1) Check Firebase current user (auto-login)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // 2) If we already saved role/fullName locally -> navigate immediately
            // (If not saved yet, you can still go to Login and it will fetch + save)
            String role = UserStorage.getRole(this);
            String fullName = UserStorage.getFullName(this);

            Fragment start;
            if ("manager".equalsIgnoreCase(role)) {
                start = ManagerHomeFragment.newInstance(fullName);
            } else if ("employee".equalsIgnoreCase(role)) {
                start = EmployeeHomeFragment.newInstance(fullName);
            } else {
                // Fallback if role not available locally yet
                start = new LoginFragment();
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, start)
                    .commit();

        } else {
            // Not logged in -> go to Login
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
        }
    }
}
