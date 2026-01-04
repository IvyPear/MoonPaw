package com.example.moonpaw.utils;

import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SleepAnalyzer {

    public static final float THRESHOLD_BAD = 6.5f;
    public static final float THRESHOLD_OK = 7.5f;

    public static final int COLOR_GOOD = Color.parseColor("#4ade80");
    public static final int COLOR_OK = Color.parseColor("#facc15");
    public static final int COLOR_BAD = Color.parseColor("#94a3b8");

    public static String getDateKey(long timestamp) {
        return "history_" + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(timestamp);
    }

    public static String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp);
    }

    public static float calculateHours(long startMs, long endMs) {
        float hours = (endMs - startMs) / (1000f * 60 * 60);
        return hours > 0 ? hours : 0;
    }

    public static float calculateDurationFromString(String startStr, String endStr) {
        try {
            String[] s = startStr.split(":");
            String[] e = endStr.split(":");
            int sMin = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
            int eMin = Integer.parseInt(e[0]) * 60 + Integer.parseInt(e[1]);
            int diff = eMin - sMin;
            if (diff < 0) diff += 1440;
            return diff / 60.0f;
        } catch (Exception ex) { return 0; }
    }

    public static int getQualityColor(float hours) {
        if (hours >= THRESHOLD_OK) return COLOR_GOOD;
        if (hours >= THRESHOLD_BAD) return COLOR_OK;
        return COLOR_BAD;
    }

    public static String getSleepLabel(float hours) {
        if (hours >= THRESHOLD_OK) return "Rất tốt – Phục hồi";
        if (hours >= THRESHOLD_BAD) return "Tạm ổn";
        return "Thiếu ngủ";
    }
}