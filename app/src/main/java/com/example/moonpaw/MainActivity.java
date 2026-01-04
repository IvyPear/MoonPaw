package com.example.moonpaw;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.fragment.CalendarFragment;
import com.example.moonpaw.fragment.HomeFragment;
import com.example.moonpaw.fragment.MusicFragment;
import com.example.moonpaw.fragment.ProfileFragment;
import com.example.moonpaw.fragment.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load HomeFragment mặc định
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Xử lý click tab
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment(); // Tạo sau
            } else if (itemId == R.id.nav_music) {
                selectedFragment = new MusicFragment(); // Đã có

            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment(); // Đã có
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}