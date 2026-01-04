package com.example.moonpaw.fragment;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.moonpaw.R;
import java.util.Locale;

public class SleepSettingsFragment extends Fragment {

    private TextView tvPickBedtime, tvPickWakeup, tvTargetDuration, tvTimeRemaining;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);

        tvPickBedtime = view.findViewById(R.id.tv_pick_bedtime);
        tvPickWakeup = view.findViewById(R.id.tv_pick_wakeup);
        tvTargetDuration = view.findViewById(R.id.tv_target_duration);
        tvTimeRemaining = view.findViewById(R.id.tv_time_remaining);

        // Load dữ liệu cũ
        String bedtime = prefs.getString("bedtime", "22:30");
        String wakeup = prefs.getString("wakeup", "06:30");
        tvPickBedtime.setText(bedtime);
        tvPickWakeup.setText(wakeup);

        calculateSleepInfo(bedtime, wakeup);

        // Click để đổi giờ ngủ
        view.findViewById(R.id.card_set_bedtime).setOnClickListener(v -> showPicker(true));
        // Click để đổi giờ dậy
        view.findViewById(R.id.card_set_wakeup).setOnClickListener(v -> showPicker(false));
    }

    private void showPicker(boolean isBedtime) {
        new TimePickerDialog(getContext(), (view, h, m)-> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            if (isBedtime) {
                tvPickBedtime.setText(time);
                prefs.edit().putString("bedtime", time).apply();
            } else {
                tvPickWakeup.setText(time);
                prefs.edit().putString("wakeup", time).apply();
            }
            calculateSleepInfo(tvPickBedtime.getText().toString(), tvPickWakeup.getText().toString());
        }, isBedtime ? 22 : 6, 30, true).show();
    }

    private void calculateSleepInfo(String sleep, String wake) {
        String[] s = sleep.split(":");
        String[] w = wake.split(":");

        int sMin = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
        int wMin = Integer.parseInt(w[0]) * 60 + Integer.parseInt(w[1]);

        int diff = wMin - sMin;
        if (diff < 0) diff += 1440; // Ngủ qua đêm (24 tiếng)

        int h = diff / 60;
        int m = diff % 60;

        tvTargetDuration.setText(h + " tiếng " + (m > 0 ? m + "m" : ""));
        tvTimeRemaining.setText(h + "h " + m + "m");
    }
}