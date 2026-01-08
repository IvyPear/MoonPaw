package com.example.moonpaw.utils;

import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepAnalyzer {

    // 1️⃣ BẢNG NGƯỠNG THỜI LƯỢNG NGỦ (Theo National Sleep Foundation)
    private static final float THRESHOLD_BAD_MAX = 6.5f;    // < 6.5h: Thiếu rõ rệt
    private static final float THRESHOLD_GOOD_MIN = 7.5f;   // 7.5h - 9h: Lý tưởng
    private static final float THRESHOLD_GOOD_MAX = 9.0f;   // > 9h: Ngủ quá nhiều

    // Mã màu UI (Giữ nguyên)
    public static final int COLOR_GOOD = Color.parseColor("#4ade80"); // Xanh lá
    public static final int COLOR_OK = Color.parseColor("#facc15");   // Vàng
    public static final int COLOR_BAD = Color.parseColor("#ef4444");  // Đỏ
    public static final int COLOR_SLEEPING = Color.parseColor("#3b82f6"); // Xanh biển

    // Enum UI (4 trạng thái cố định)
    public enum SleepState {
        SLEEPING, GOOD, OK, BAD
    }

    // Enum Nguyên nhân (Để hiển thị câu thoại)
    public enum SleepIssue {
        NONE,           // Không vấn đề
        SHORT_SLEEP,    // Thiếu giờ
        LATE_SLEEP,     // Ngủ muộn (Ảnh hưởng nhịp sinh học)
        OVER_SLEEP,     // Ngủ quá nhiều
        SHORT_AND_LATE, // Combo: Thiếu + Muộn
        MILD_SHORT      // Thiếu nhẹ (6.5 - 7.5h)
    }

    // --- HÀM 1: PHÂN TÍCH NGUYÊN NHÂN (Dùng cho Text gợi ý) ---
    public static SleepIssue analyzeSleepIssue(float hours, String bedTimeStr) {
        boolean isSevereShort = hours < THRESHOLD_BAD_MAX;
        boolean isMildShort = hours >= THRESHOLD_BAD_MAX && hours < THRESHOLD_GOOD_MIN;
        boolean isOver = hours > THRESHOLD_GOOD_MAX;
        boolean isLate = isLateSleep(bedTimeStr);

        if (isSevereShort && isLate) return SleepIssue.SHORT_AND_LATE;
        if (isSevereShort) return SleepIssue.SHORT_SLEEP;
        if (isOver) return SleepIssue.OVER_SLEEP;

        // Chỉ báo lỗi ngủ muộn nếu thời lượng ngủ đã ĐỦ (để tránh ưu tiên nhầm)
        // Nếu thiếu giờ thì ưu tiên báo thiếu giờ. Nếu đủ giờ mà ngủ muộn -> LATE_SLEEP
        if (isLate) return SleepIssue.LATE_SLEEP;

        if (isMildShort) return SleepIssue.MILD_SHORT;

        return SleepIssue.NONE;
    }

    // --- HÀM 2: QUYẾT ĐỊNH TRẠNG THÁI UI (GOOD/OK/BAD) ---
    // Logic tham chiếu Bảng 3: Bảng tổng hợp trạng thái
    public static SleepState evaluateSleepState(float hours, boolean isSleeping, String bedTimeStr) {
        if (isSleeping) return SleepState.SLEEPING;

        boolean isLate = isLateSleep(bedTimeStr);

        // 1. BAD: Thiếu ngủ nhiều (< 6.5h) Bất kể giờ ngủ
        if (hours < THRESHOLD_BAD_MAX) {
            return SleepState.BAD;
        }

        // 2. OK: Rơi vào các trường hợp "Chưa tối ưu"
        // - Thiếu nhẹ (6.5 - 7.5h)
        // - Ngủ quá nhiều (> 9h)
        // - Ngủ đủ (7.5 - 9h) NHƯNG Ngủ muộn (Nhịp sinh học lệch)
        if ((hours >= THRESHOLD_BAD_MAX && hours < THRESHOLD_GOOD_MIN) ||
                (hours > THRESHOLD_GOOD_MAX) ||
                (isLate)) {
            return SleepState.OK;
        }

        // 3. GOOD: Đủ giờ (7.5 - 9h) VÀ Không ngủ muộn (Sớm/Đúng giờ)
        return SleepState.GOOD;
    }

    // --- 2️⃣ BẢNG ĐÁNH GIÁ GIỜ ĐI NGỦ (Bedtime Logic) ---
    // Ngủ muộn: Sau 00:30 (Bắt đầu giảm chất lượng)
    private static boolean isLateSleep(String bedTimeStr) {
        try {
            if (bedTimeStr == null) return false;
            String[] parts = bedTimeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            // Định nghĩa ngủ muộn: Từ 00:30 đến 05:00 sáng
            if (hour == 0 && minute >= 30) return true; // 00:30 - 00:59
            if (hour >= 1 && hour <= 5) return true;    // 01:00 - 05:00

            return false;
        } catch (Exception e) { return false; }
    }

    // --- GIỮ NGUYÊN CÁC HÀM CŨ ĐỂ KHÔNG LỖI FILE KHÁC ---
    public static SleepState evaluateSleepState(float hours, boolean isSleeping) {
        return evaluateSleepState(hours, isSleeping, "00:00");
    }
    public static String getDateKey(long timestamp) {
        return "history_" + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(timestamp));
    }
    public static String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
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
        switch (evaluateSleepState(hours, false)) {
            case GOOD: return COLOR_GOOD;
            case OK: return COLOR_OK;
            case BAD: return COLOR_BAD;
            default: return COLOR_OK;
        }
    }
    public static String getSleepLabel(float hours) {
        switch (evaluateSleepState(hours, false)) {
            case GOOD: return "Lý tưởng";
            case OK: return "Chưa tối ưu";
            case BAD: return "Cảnh báo";
            default: return "--";
        }
    }
}