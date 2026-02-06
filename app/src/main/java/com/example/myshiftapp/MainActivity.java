package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavController navController;

    private TextView menuProfile, menuSchedule, menuAttendance, menuMonthly, menuPayslips;
    private TextView menuManagerSettings, menuLogout;

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

// ===== Menu items =====
        menuProfile = findViewById(R.id.menuProfile);
        menuSchedule = findViewById(R.id.menuSchedule);
        menuAttendance = findViewById(R.id.menuAttendance);
        menuMonthly = findViewById(R.id.menuMonthly);
        menuPayslips = findViewById(R.id.menuPayslips);
        menuManagerSettings = findViewById(R.id.menuManagerSettings);
        menuLogout = findViewById(R.id.menuLogout);

// ===== Click listeners (THIS is what was missing) =====
        menuProfile.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Profile clicked");
            safeNavigate(R.id.employeeProfileFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuSchedule.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Schedule clicked");
            safeNavigate(R.id.employeeScheduleFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuAttendance.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Attendance clicked");
            safeNavigate(R.id.employeeAttendanceFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuMonthly.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Monthly clicked");
            safeNavigate(R.id.employeeMonthlyDetailsFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuPayslips.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Payslips clicked");
            safeNavigate(R.id.employeePayslipsFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuManagerSettings.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "ManagerSettings clicked");
            safeNavigate(R.id.managerSettingsFragment);
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        menuLogout.setOnClickListener(v -> {
            Log.d("MENU_CLICK", "Logout clicked");

            FirebaseAuth.getInstance().signOut();
            UserStorage.clear(this);

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();

            navController.navigate(R.id.loginFragment, null, navOptions);
            drawerLayout.closeDrawer(GravityCompat.END);

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

    private void updateDrawerMenu() {
        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean isManager = UserStorage.isManager(this);

        int dest = (navController.getCurrentDestination() != null)
                ? navController.getCurrentDestination().getId() : -1;

        Log.d("MENU", "updateDrawerMenu: isLoggedIn=" + isLoggedIn
                + " uidPref=" + UserStorage.getUid(this)
                + " role=" + UserStorage.getRole(this)
                + " isManager=" + isManager
                + " dest=" + dest);

        boolean onLoginScreen = (dest == R.id.loginFragment);

        if (!isLoggedIn || onLoginScreen) {
            menuProfile.setVisibility(View.GONE);
            menuSchedule.setVisibility(View.GONE);
            menuAttendance.setVisibility(View.GONE);
            menuMonthly.setVisibility(View.GONE);
            menuPayslips.setVisibility(View.GONE);
            menuManagerSettings.setVisibility(View.GONE);
            menuLogout.setVisibility(View.GONE);
            return;
        }

        menuLogout.setVisibility(View.VISIBLE);

        if (isManager) {
            menuProfile.setVisibility(View.GONE);
            menuSchedule.setVisibility(View.GONE);
            menuAttendance.setVisibility(View.GONE);
            menuMonthly.setVisibility(View.GONE);
            menuPayslips.setVisibility(View.GONE);
            menuManagerSettings.setVisibility(View.VISIBLE);
        } else {
            menuProfile.setVisibility(View.VISIBLE);
            menuSchedule.setVisibility(View.VISIBLE);
            menuAttendance.setVisibility(View.VISIBLE);
            menuMonthly.setVisibility(View.VISIBLE);
            menuPayslips.setVisibility(View.VISIBLE);
            menuManagerSettings.setVisibility(View.GONE);
        }
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