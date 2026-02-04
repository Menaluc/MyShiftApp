package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found. Check activity_main.xml id = nav_host_fragment");
        }

        navController = navHostFragment.getNavController();

        TextView menuProfile = findViewById(R.id.menuProfile);
        TextView menuSchedule = findViewById(R.id.menuSchedule);
        TextView menuAttendance = findViewById(R.id.menuAttendance);
        TextView menuMonthly = findViewById(R.id.menuMonthly);
        TextView menuPayslips = findViewById(R.id.menuPayslips);
        TextView menuManagerSettings = findViewById(R.id.menuManagerSettings); // ✅ NEW
        TextView menuLogout = findViewById(R.id.menuLogout);

        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean isManager = UserStorage.isManager(this);

        Log.d("MENU", "isLoggedIn=" + isLoggedIn
                + " uidPref=" + UserStorage.getUid(this)
                + " role=" + UserStorage.getRole(this)
                + " isManager=" + isManager
                + " dest=" + (navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId() : -1));

        if (!isLoggedIn) {
            menuProfile.setVisibility(View.GONE);
            menuSchedule.setVisibility(View.GONE);
            menuAttendance.setVisibility(View.GONE);
            menuMonthly.setVisibility(View.GONE);
            menuPayslips.setVisibility(View.GONE);
            menuManagerSettings.setVisibility(View.GONE); // ✅ NEW
            menuLogout.setVisibility(View.GONE);
        } else {
            menuLogout.setVisibility(View.VISIBLE);

            // כרגע: חבילה 1 = עובד
            if (isManager) {
                menuProfile.setVisibility(View.GONE);
                menuSchedule.setVisibility(View.GONE);
                menuAttendance.setVisibility(View.GONE);
                menuMonthly.setVisibility(View.GONE);
                menuPayslips.setVisibility(View.GONE);

                menuManagerSettings.setVisibility(View.VISIBLE); // ✅ NEW
            } else {
                menuProfile.setVisibility(View.VISIBLE);
                menuSchedule.setVisibility(View.VISIBLE);
                menuAttendance.setVisibility(View.VISIBLE);
                menuMonthly.setVisibility(View.VISIBLE);
                menuPayslips.setVisibility(View.VISIBLE);

                menuManagerSettings.setVisibility(View.GONE); // ✅ NEW
            }
        }

        menuProfile.setOnClickListener(v -> {
            safeNavigate(R.id.employeeProfileFragment);
            drawerLayout.closeDrawers();
        });

        menuSchedule.setOnClickListener(v -> {
            safeNavigate(R.id.employeeScheduleFragment);
            drawerLayout.closeDrawers();
        });

        menuAttendance.setOnClickListener(v -> {
            safeNavigate(R.id.employeeAttendanceFragment);
            drawerLayout.closeDrawers();
        });

        menuMonthly.setOnClickListener(v -> {
            safeNavigate(R.id.employeeMonthlyDetailsFragment);
            drawerLayout.closeDrawers();
        });

        menuPayslips.setOnClickListener(v -> {
            safeNavigate(R.id.employeePayslipsFragment);
            drawerLayout.closeDrawers();
        });

        // ✅ NEW: Manager Settings navigation (only visible for manager anyway)
        menuManagerSettings.setOnClickListener(v -> {
            safeNavigate(R.id.managerSettingsFragment);
            drawerLayout.closeDrawers();
        });

        menuLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            UserStorage.clear(this);

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();

            navController.navigate(R.id.loginFragment, null, navOptions);
            drawerLayout.closeDrawers();
        });
    }

    private void safeNavigate(int destinationId) {
        if (navController.getCurrentDestination() == null) return;
        if (navController.getCurrentDestination().getId() == destinationId) return;
        navController.navigate(destinationId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}