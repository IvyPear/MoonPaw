package com.example.moonpaw.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;
import com.example.moonpaw.utils.SleepAnalyzer;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvDate, tvSleepStatus, tvSleepTip, tvBedtime, tvWakeup, tvStreak;
    private Button btnStartSleep, btnWakeUp;
    private MaterialCardView cardStartSleep, cardWakeUp;

    private ImageView imgCatAvatar;
    private View viewCircleBg, viewCircleBorder;

    private SharedPreferences prefs;

    private final long TIME_TO_RESET_SECONDS = 60; // Test mode
    private Handler refreshHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);

        initViews(view);
        setupListeners(view);

        checkAndPerformReset();
        refreshUI();
        startAutoRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            }
        }
        refreshUI();
    }

    private void initViews(View v) {
        tvDate = v.findViewById(R.id.tv_date);
        tvSleepStatus = v.findViewById(R.id.tv_sleep_status);
        tvSleepTip = v.findViewById(R.id.tv_sleep_tip);
        tvBedtime = v.findViewById(R.id.tv_bedtime);
        tvWakeup = v.findViewById(R.id.tv_wakeup);
        tvStreak = v.findViewById(R.id.tv_streak);

        btnStartSleep = v.findViewById(R.id.btn_start_sleep);
        btnWakeUp = v.findViewById(R.id.btn_wake_up);
        cardStartSleep = v.findViewById(R.id.card_start_sleep);
        cardWakeUp = v.findViewById(R.id.card_wake_up);

        imgCatAvatar = v.findViewById(R.id.img_cat_avatar);
        viewCircleBg = v.findViewById(R.id.view_circle_bg);
        viewCircleBorder = v.findViewById(R.id.view_circle_border);
    }

    private void setupListeners(View v) {
        btnStartSleep.setOnClickListener(view -> {
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putLong("sleep_start", now)
                    .putString("bedtime", SleepAnalyzer.formatTime(now))
                    .putBoolean("cycle_completed", false)
                    .apply();
            refreshUI();
            Toast.makeText(getContext(), "Chúc bạn ngủ ngon! 🌙", Toast.LENGTH_SHORT).show();
        });

        btnWakeUp.setOnClickListener(view -> {
            long end = System.currentTimeMillis();
            long start = prefs.getLong("sleep_start", 0);

            if (start > 0) {
                float hours = SleepAnalyzer.calculateHours(start, end);
                String dateKey = SleepAnalyzer.getDateKey(start);

                updateStreakLogic(end);

                prefs.edit()
                        .putFloat(dateKey, hours)
                        .putString("wakeup", SleepAnalyzer.formatTime(end))
                        .putLong("last_completion_time", end)
                        .putFloat("last_duration", hours)
                        .putBoolean("cycle_completed", true)
                        .remove("sleep_start")
                        .apply();

                refreshUI();
                Toast.makeText(getContext(), "Đã lưu " + String.format("%.1f", hours) + "h!", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigation logic...
        try {
            View cardSchedule = v.findViewById(R.id.card_sleep_schedule);
            if (cardSchedule != null) cardSchedule.setOnClickListener(view -> navigateTo(new SleepSettingsFragment()));
            View cardNap = v.findViewById(R.id.card_sleep_bu);
            if (cardNap != null) cardNap.setOnClickListener(view -> navigateTo(new PowerNapFragment()));
            View cardBreath = v.findViewById(R.id.card_breathing);
            if (cardBreath != null) cardBreath.setOnClickListener(view -> navigateTo(new BreathingFragment()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * HÀM MỚI: Cập nhật UI theo Bảng đánh giá khoa học
     */
    private void updateCatState(boolean isSleeping, float hours) {
        // Lấy giờ đi ngủ thực tế để phân tích nhịp sinh học
        String actualBedTime = prefs.getString("bedtime", "23:00");

        // 1. Xác định UI State (Good/OK/Bad) dựa trên Duration + Bedtime
        SleepAnalyzer.SleepState state = SleepAnalyzer.evaluateSleepState(hours, isSleeping, actualBedTime);

        // 2. Xác định Nguyên nhân cụ thể để hiển thị Tip Text
        SleepAnalyzer.SleepIssue issue = SleepAnalyzer.analyzeSleepIssue(hours, actualBedTime);

        int imgRes;
        int colorRes;
        String statusText;
        String tipText;

        switch (state) {
            case SLEEPING:
                imgRes = R.drawable.cat_sleeping;
                colorRes = SleepAnalyzer.COLOR_SLEEPING;
                statusText = "Đang ngủ";
                tipText = "Mèo đang canh cho bạn...";
                break;

            case GOOD:
                // Thỏa mãn: 7.5-9h VÀ Ngủ sớm
                imgRes = R.drawable.cat_happy;
                colorRes = SleepAnalyzer.COLOR_GOOD;
                statusText = "Lý tưởng";
                // Bảng 4: "Không vấn đề"
                tipText = "Giữ thói quen ngủ đều đặn giúp bạn duy trì năng lượng.";
                break;

            case OK:
                // Bao gồm: Thiếu nhẹ, Ngủ quá nhiều, Hoặc Ngủ đủ nhưng muộn
                imgRes = R.drawable.cat_tired;
                colorRes = SleepAnalyzer.COLOR_OK;
                statusText = "Tạm ổn";

                // Map câu thoại theo Bảng 4
                if (issue == SleepAnalyzer.SleepIssue.LATE_SLEEP) {
                    tipText = "Bạn nên đi ngủ sớm hơn để cải thiện nhịp sinh học.";
                } else if (issue == SleepAnalyzer.SleepIssue.OVER_SLEEP) {
                    tipText = "Ngủ quá nhiều có thể khiến bạn cảm thấy uể oải.";
                } else if (issue == SleepAnalyzer.SleepIssue.MILD_SHORT) {
                    tipText = "Bạn nên ngủ thêm để cơ thể phục hồi tốt hơn.";
                } else {
                    // Fallback
                    tipText = "Cơ thể chưa hồi phục hoàn toàn.";
                }
                break;

            case BAD:
                // Bao gồm: Thiếu nhiều (<6.5h)
                imgRes = R.drawable.cat_exhausted;
                colorRes = SleepAnalyzer.COLOR_BAD;
                statusText = "Cần chú ý"; // Hoặc "Thiếu ngủ"

                if (issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) {
                    tipText = "Ngủ muộn và thiếu giờ có thể gây mệt mỏi kéo dài.";
                } else {
                    // Mặc định cho SHORT_SLEEP
                    tipText = "Bạn nên ngủ thêm để cơ thể phục hồi tốt hơn.";
                }
                break;

            default:
                imgRes = R.drawable.cat_happy;
                colorRes = Color.GRAY;
                statusText = "--";
                tipText = "";
                break;
        }

        // Cập nhật UI (Animation mờ)
        if (imgCatAvatar != null) {
            Object tag = imgCatAvatar.getTag();
            if (tag == null || !tag.equals(imgRes)) {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(200);
                fadeOut.setFillAfter(true);
                imgCatAvatar.startAnimation(fadeOut);

                final int finalImgRes = imgRes;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    imgCatAvatar.setImageResource(finalImgRes);
                    imgCatAvatar.setTag(finalImgRes);
                    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                    fadeIn.setDuration(200);
                    fadeIn.setFillAfter(true);
                    imgCatAvatar.startAnimation(fadeIn);
                }, 200);
            }
        }

        if (viewCircleBg != null) viewCircleBg.setBackgroundTintList(ColorStateList.valueOf(colorRes));
        if (viewCircleBorder != null) viewCircleBorder.getBackground().setTint(colorRes);
        if (tvSleepStatus != null) {
            tvSleepStatus.setText(statusText);
            tvSleepStatus.setBackgroundTintList(ColorStateList.valueOf(colorRes));
        }
        if (tvSleepTip != null) tvSleepTip.setText(tipText);
    }

    private void updateStreakLogic(long currentTimeMs) {
        int currentStreak = prefs.getInt("streak_count", 0);
        long lastCompletion = prefs.getLong("last_completion_time", 0);

        if (lastCompletion == 0) {
            currentStreak = 1;
        } else {
            Calendar now = Calendar.getInstance(); now.setTimeInMillis(currentTimeMs);
            Calendar last = Calendar.getInstance(); last.setTimeInMillis(lastCompletion);

            now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0);
            last.set(Calendar.HOUR_OF_DAY, 0); last.set(Calendar.MINUTE, 0);

            long diffDays = (now.getTimeInMillis() - last.getTimeInMillis()) / (24 * 60 * 60 * 1000);
            if (diffDays == 1) currentStreak++;
            else if (diffDays > 1) currentStreak = 1;
        }
        prefs.edit().putInt("streak_count", currentStreak).apply();
    }

    private void checkAndPerformReset() {
        long lastComp = prefs.getLong("last_completion_time", 0);
        if (lastComp == 0) return;

        if ((System.currentTimeMillis() - lastComp) / 1000 >= TIME_TO_RESET_SECONDS) {
            prefs.edit().putBoolean("cycle_completed", false).remove("last_completion_time").apply();
            if (getActivity() != null) getActivity().runOnUiThread(this::refreshUI);
        }
    }

    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndPerformReset();
                refreshHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void refreshUI() {
        tvDate.setText(new SimpleDateFormat("EEEE, dd/MM", Locale.getDefault()).format(Calendar.getInstance().getTime()));
        tvStreak.setText(String.valueOf(prefs.getInt("streak_count", 0)));

        String bedTimeStr = prefs.getString("bedtime", "23:00");
        String wakeUpStr = prefs.getString("wakeup", "07:00");
        tvBedtime.setText(bedTimeStr);
        tvWakeup.setText(wakeUpStr);

        boolean isCompleted = prefs.getBoolean("cycle_completed", false);
        long isSleeping = prefs.getLong("sleep_start", 0);

        if (isCompleted) {
            updateButtonState(cardStartSleep, btnStartSleep, true);
            updateButtonState(cardWakeUp, btnWakeUp, true);

            float lastDuration = prefs.getFloat("last_duration", 8.0f);
            updateCatState(false, lastDuration); // State: Completed (Show Report)

        } else if (isSleeping > 0) {
            updateButtonState(cardStartSleep, btnStartSleep, true);
            updateButtonState(cardWakeUp, btnWakeUp, false);

            updateCatState(true, 0); // State: Sleeping

        } else {
            updateButtonState(cardStartSleep, btnStartSleep, false);
            updateButtonState(cardWakeUp, btnWakeUp, true);

            // Chế độ dự đoán (Prediction) - Chỉ hiện thị giờ dự kiến, không đánh giá
            float predictedHours = SleepAnalyzer.calculateDurationFromString(bedTimeStr, wakeUpStr);

            // Dùng logic cũ để set màu tạm thời hoặc set màu trung tính
            updateCatState(false, predictedHours);

            // Override text riêng cho trạng thái dự kiến
            tvSleepTip.setText("Dự kiến: " + String.format("%.1f", predictedHours) + "h");
        }
    }

    private void updateButtonState(MaterialCardView card, Button btn, boolean isLocked) {
        card.setCardBackgroundColor(Color.parseColor(isLocked ? "#1e293b" : "#6366f1"));
        btn.setEnabled(!isLocked);
        btn.setAlpha(isLocked ? 0.5f : 1.0f);
    }

    private void navigateTo(Fragment f) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacksAndMessages(null);
    }
}