package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavController navController;

    // Employee menu items
    private TextView menuSchedule, menuAttendance, menuMonthly, menuPayslips;

    // Manager menu items
    private TextView menuManagerSettings, menuManagerEmployees, menuManagerBuildSchedule;
    private TextView menuManagerAttendanceApprovals, menuManagerPayslips;

    // Shared
    private TextView menuLogout;
    private View dividerEmployeeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

// DrawerLayout from activity_main.xml
        drawerLayout = findViewById(R.id.drawerLayout);

// NavHost + NavController
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            throw new IllegalStateException(
                    "NavHostFragment not found. Check activity_main.xml id = nav_host_fragment"
            );
        }

        navController = navHostFragment.getNavController();

// ===== Find views =====
// Employee
        menuSchedule = findViewById(R.id.menuSchedule);
        menuAttendance = findViewById(R.id.menuAttendance);
        menuMonthly = findViewById(R.id.menuMonthly);
        menuPayslips = findViewById(R.id.menuPayslips);

// Manager
        menuManagerSettings = findViewById(R.id.menuManagerSettings);
        menuManagerEmployees = findViewById(R.id.menuManagerEmployees);
        menuManagerBuildSchedule = findViewById(R.id.menuManagerBuildSchedule);
        menuManagerAttendanceApprovals = findViewById(R.id.menuManagerAttendanceApprovals);
        menuManagerPayslips = findViewById(R.id.menuManagerPayslips);

// Shared
        menuLogout = findViewById(R.id.menuLogout);

// Divider between employee/manager sections (exists in activity_main.xml)
        dividerEmployeeManager = findViewById(R.id.dividerEmployeeManager);

// ===== Click listeners =====
// Employee menu navigation
        menuSchedule.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Employee Schedule clicked");
            safeNavigate(R.id.employeeScheduleFragment);
            closeRightDrawer();
        });

        menuAttendance.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Employee Attendance clicked");
            safeNavigate(R.id.employeeAttendanceFragment);
            closeRightDrawer();
        });

        menuMonthly.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Employee Monthly clicked");
            safeNavigate(R.id.employeeMonthlyDetailsFragment);
            closeRightDrawer();
        });

        menuPayslips.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Employee Payslips clicked");
            safeNavigate(R.id.employeePayslipsFragment);
            closeRightDrawer();
        });

// Manager menu navigation
        menuManagerSettings.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Manager Settings clicked");
            safeNavigate(R.id.managerSettingsFragment);
            closeRightDrawer();
        });

        menuManagerEmployees.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Manager Employees clicked");
            safeNavigate(R.id.managerEmployeesFragment);
            closeRightDrawer();
        });

        menuManagerBuildSchedule.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Manager BuildSchedule clicked");
            safeNavigate(R.id.managerBuildScheduleFragment);
            closeRightDrawer();
        });

        menuManagerAttendanceApprovals.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Manager AttendanceApprovals clicked");
            safeNavigate(R.id.managerAttendanceApprovalsFragment);
            closeRightDrawer();
        });

        menuManagerPayslips.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Manager Payslips clicked");
// Option A: Payslips per employee (using the existing EmployeePayslipsFragment)
            safeNavigate(R.id.employeePayslipsFragment);
            closeRightDrawer();
        });

// Logout (shared)
        menuLogout.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Logout clicked");

            FirebaseAuth.getInstance().signOut();
            UserStorage.clear(this);

// Pop everything and go to login
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();

            navController.navigate(R.id.loginFragment, null, navOptions);
            closeRightDrawer();

            updateDrawerMenu();
        });

// Initial refresh
        updateDrawerMenu();

// Refresh menu on every navigation change
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateDrawerMenu();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDrawerMenu();
    }

    /**
     * Called by fragments (e.g., ManagerHomeFragment) to open the shared right drawer.
     */
    public void openRightDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    public void closeRightDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    private void updateDrawerMenu() {
        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean isManager = UserStorage.isManager(this);

        int dest = (navController.getCurrentDestination() != null)
                ? navController.getCurrentDestination().getId() : -1;

        boolean onLoginScreen = (dest == R.id.loginFragment);

        Log.d("MENU", "updateDrawerMenu: isLoggedIn=" + isLoggedIn
                + " role=" + UserStorage.getRole(this)
                + " isManager=" + isManager
                + " dest=" + dest);

// If not logged in (or on login screen) -> hide everything
        if (!isLoggedIn || onLoginScreen) {
            setEmployeeMenuVisible(false);
            setManagerMenuVisible(false);
            if (dividerEmployeeManager != null) dividerEmployeeManager.setVisibility(View.GONE);
            menuLogout.setVisibility(View.GONE);
            return;
        }

// Logged in -> show logout
        menuLogout.setVisibility(View.VISIBLE);

// Show only the relevant menu group
        if (isManager) {
            setEmployeeMenuVisible(false);
            setManagerMenuVisible(true);
        } else {
            setEmployeeMenuVisible(true);
            setManagerMenuVisible(false);
        }

// Divider should appear only if BOTH sections are visible (usually never),
// but keeping logic clean to avoid "floating divider" UI bug.
        updateDividerVisibility();
    }

    private void setEmployeeMenuVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        menuSchedule.setVisibility(v);
        menuAttendance.setVisibility(v);
        menuMonthly.setVisibility(v);
        menuPayslips.setVisibility(v);
    }

    private void setManagerMenuVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        menuManagerSettings.setVisibility(v);
        menuManagerEmployees.setVisibility(v);
        menuManagerBuildSchedule.setVisibility(v);
        menuManagerAttendanceApprovals.setVisibility(v);
        menuManagerPayslips.setVisibility(v);
    }

    private void updateDividerVisibility() {
        if (dividerEmployeeManager == null) return;

        boolean employeeVisible = (menuSchedule.getVisibility() == View.VISIBLE);
        boolean managerVisible = (menuManagerSettings.getVisibility() == View.VISIBLE);

        dividerEmployeeManager.setVisibility(employeeVisible && managerVisible ? View.VISIBLE : View.GONE);
    }

    private void safeNavigate(int destinationId) {
        if (navController == null || navController.getCurrentDestination() == null) return;
        if (navController.getCurrentDestination().getId() == destinationId) return;

        try {
            navController.navigate(destinationId);
        } catch (Exception e) {
            Log.e("MENU_NAV", "Navigation failed", e);
            Toast.makeText(this, "Navigation failed (check Logcat)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}