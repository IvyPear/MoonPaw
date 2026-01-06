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

    // THAY ĐỔI: Sử dụng ImageView thay vì Lottie, thêm view để đổi màu nền
    private ImageView imgCatAvatar;
    private View viewCircleBg, viewCircleBorder;

    private SharedPreferences prefs;

    // Reset sau 60 giây (Test). Khi chạy thật sửa thành: 8 * 3600
    private final long TIME_TO_RESET_SECONDS = 60;
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
        // setupLottieAnimation(); -> BỎ LOTTIE
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
        // Không cần resume animation nữa
        refreshUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Không cần pause animation nữa
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

        // Ánh xạ ImageView và các view vòng tròn
        imgCatAvatar = v.findViewById(R.id.img_cat_avatar);
        viewCircleBg = v.findViewById(R.id.view_circle_bg);
        viewCircleBorder = v.findViewById(R.id.view_circle_border);
    }

    // BỎ HÀM setupLottieAnimation()

    private void setupListeners(View v) {
        // 1. Nút Bắt đầu ngủ
        btnStartSleep.setOnClickListener(view -> {
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putLong("sleep_start", now)
                    .putString("bedtime", SleepAnalyzer.formatTime(now))
                    .putBoolean("cycle_completed", false)
                    .apply();

            // Logic hình ảnh sẽ được xử lý trong refreshUI -> updateCatState
            refreshUI();
            Toast.makeText(getContext(), "Chúc bạn ngủ ngon! 🌙", Toast.LENGTH_SHORT).show();
        });

        // 2. Nút Tôi đã dậy
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
                        // LƯU THÊM DỮ LIỆU ĐỂ HIỂN THỊ MÈO SAU KHI RESET APP
                        .putFloat("last_duration", hours)
                        .putBoolean("cycle_completed", true)
                        .remove("sleep_start")
                        .apply();

                refreshUI();
                Toast.makeText(getContext(), "Đã lưu " + String.format("%.1f", hours) + "h!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Các nút điều hướng giữ nguyên
        try {
            View cardSchedule = v.findViewById(R.id.card_sleep_schedule);
            if (cardSchedule != null) cardSchedule.setOnClickListener(view -> navigateTo(new SleepSettingsFragment()));

            View cardNap = v.findViewById(R.id.card_sleep_bu);
            if (cardNap != null) cardNap.setOnClickListener(view -> navigateTo(new PowerNapFragment()));

            View cardBreath = v.findViewById(R.id.card_breathing);
            if (cardBreath != null) cardBreath.setOnClickListener(view -> navigateTo(new BreathingFragment()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // BỎ animateCatToSleep() và animateCatToWakeUp()

    /**
     * HÀM MỚI: Cập nhật trạng thái Mèo và Màu sắc
     */
    private void updateCatState(boolean isSleeping, float hours) {
        int imgRes;
        int colorRes;
        String statusText;

        if (isSleeping) {
            // Trạng thái 1: Đang ngủ (Xanh biển)
            imgRes = R.drawable.cat_sleeping;
            colorRes = Color.parseColor("#2196F3"); // Blue
            statusText = "Đang ngủ";
        } else {
            // Đã dậy: Kiểm tra thời gian ngủ
            if (hours >= 7) {
                // Trạng thái 2: Ngủ đủ (Xanh lá)
                imgRes = R.drawable.cat_happy;
                colorRes = Color.parseColor("#10B981"); // Green
                statusText = "Ngủ đủ";
            } else if (hours >= 5) {
                // Trạng thái 3: Hơi mệt (Vàng nhạt)
                imgRes = R.drawable.cat_tired;
                colorRes = Color.parseColor("#FBBF24"); // Yellow
                statusText = "Hơi mệt";
            } else {
                // Trạng thái 4: Thiếu ngủ trầm trọng (Đỏ/Đỏ cam)
                imgRes = R.drawable.cat_exhausted;
                colorRes = Color.parseColor("#EF4444"); // Red
                statusText = "Thiếu ngủ";
            }
        }

        // Cập nhật hình ảnh
        if (imgCatAvatar != null) {
            imgCatAvatar.setImageResource(imgRes);
        }

        // Cập nhật màu sắc các vòng tròn
        if (viewCircleBg != null) {
            viewCircleBg.setBackgroundTintList(ColorStateList.valueOf(colorRes));
        }
        if (viewCircleBorder != null) {
            // view_circle_border là drawable shape, dùng setTint để đổi màu viền/nền của shape đó
            viewCircleBorder.getBackground().setTint(colorRes);
        }

        // Cập nhật Text trạng thái và màu Badge
        if (tvSleepStatus != null) {
            tvSleepStatus.setText(statusText);
            tvSleepStatus.setBackgroundTintList(ColorStateList.valueOf(colorRes));
        }
    }

    private void updateStreakLogic(long currentTimeMs) {
        int currentStreak = prefs.getInt("streak_count", 0);
        long lastCompletion = prefs.getLong("last_completion_time", 0);

        if (lastCompletion == 0) {
            currentStreak = 1;
        } else {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(currentTimeMs);
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(lastCompletion);

            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            last.set(Calendar.HOUR_OF_DAY, 0);
            last.set(Calendar.MINUTE, 0);

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
            prefs.edit()
                    .putBoolean("cycle_completed", false)
                    .remove("last_completion_time")
                    .apply();
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshUI);
            }
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
        tvDate.setText(new SimpleDateFormat("EEEE, dd/MM", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));
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

            // Lấy thời lượng ngủ vừa lưu để hiển thị mèo tương ứng
            float lastDuration = prefs.getFloat("last_duration", 8.0f);
            updateCatState(false, lastDuration); // false = đã dậy

            tvSleepTip.setText("Reset sau 1 phút...");

        } else if (isSleeping > 0) {
            updateButtonState(cardStartSleep, btnStartSleep, true);
            updateButtonState(cardWakeUp, btnWakeUp, false);

            updateCatState(true, 0); // true = đang ngủ

            tvSleepTip.setText("Mèo Mun đang canh...");

        } else {
            updateButtonState(cardStartSleep, btnStartSleep, false);
            updateButtonState(cardWakeUp, btnWakeUp, true);

            // Trạng thái bình thường/dự kiến
            float predictedHours = SleepAnalyzer.calculateDurationFromString(bedTimeStr, wakeUpStr);
            updateCatState(false, predictedHours);

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