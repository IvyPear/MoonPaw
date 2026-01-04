package com.example.moonpaw.fragment;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;
import com.example.moonpaw.utils.NapReceiver;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class PowerNapFragment extends Fragment {

    private int napMinutes = 25;
    private TextView tvMinutes;
    private View breathingGlow;
    private MaterialButton[] quickButtons;
    private MaterialButton btnStart, btnWake;

    // UI Countdown
    private LinearLayout containerSetup, containerCountdown;
    private TextView tvCountdownTime;
    private ProgressBar progressBarNap;
    private CountDownTimer countdownTimer;
    private boolean isRunning = false;

    // Bộ nhớ để lưu trạng thái
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_power_nap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo bộ nhớ
        prefs = requireContext().getSharedPreferences("NapPrefs", Context.MODE_PRIVATE);

        initViews(view);
        startBreathingAnimation();

        // --- QUAN TRỌNG: KIỂM TRA XEM CÓ ĐANG NGỦ DỞ KHÔNG ---
        checkExistingNap();
        // -----------------------------------------------------

        view.findViewById(R.id.btn_back).setOnClickListener(v -> navigateBack());
        setupTimeControls(view);

        btnStart.setOnClickListener(v -> {
            if (isRunning) {
                cancelAlarmAndTimer(); // Đang chạy mà bấm -> Hủy
            } else {
                if (checkNotificationPermission()) {
                    startRealAlarm(napMinutes);
                    startNewNapSession(napMinutes); // Bắt đầu mới
                }
            }
        });

        btnWake.setOnClickListener(v -> {
            cancelAlarmAndTimer();
            navigateBack();
        });
    }

    // --- LOGIC 1: BẮT ĐẦU MỚI VÀ LƯU VÀO BỘ NHỚ ---
    private void startNewNapSession(int minutes) {
        long durationMillis = minutes * 60 * 1000;
        long endTime = System.currentTimeMillis() + durationMillis;

        // Lưu lại giờ kết thúc
        prefs.edit()
                .putBoolean("is_napping", true)
                .putLong("nap_end_time", endTime)
                .putLong("nap_total_duration", durationMillis)
                .apply();

        runCountdownUI(durationMillis, durationMillis);
    }

    // --- LOGIC 2: KHÔI PHỤC TRẠNG THÁI KHI QUAY LẠI ---
    private void checkExistingNap() {
        boolean isNapping = prefs.getBoolean("is_napping", false);
        if (isNapping) {
            long endTime = prefs.getLong("nap_end_time", 0);
            long totalDuration = prefs.getLong("nap_total_duration", 1);
            long remainingTime = endTime - System.currentTimeMillis();

            if (remainingTime > 0) {
                // Vẫn chưa hết giờ -> Chạy tiếp từ số phút còn lại
                runCountdownUI(remainingTime, totalDuration);
            } else {
                // Đã hết giờ trong lúc thoát app -> Reset
                resetUIState();
                // Xóa bộ nhớ
                prefs.edit().clear().apply();
            }
        } else {
            // Không có dữ liệu cũ -> Hiện giao diện chọn giờ
            updateUI();
        }
    }

    // --- LOGIC 3: CHẠY ĐỒNG HỒ ĐẾM NGƯỢC ---
    private void runCountdownUI(long millisToCount, long totalDuration) {
        isRunning = true;

        // Chuyển giao diện sang Đếm ngược
        containerSetup.setVisibility(View.GONE);
        containerCountdown.setVisibility(View.VISIBLE);

        btnStart.setText("Dừng lại");
        btnStart.setIconResource(R.drawable.ic_close); // Icon X
        btnStart.setBackgroundColor(Color.parseColor("#ef4444"));

        progressBarNap.setMax((int) totalDuration);

        // Hủy timer cũ nếu có để tránh chồng chéo
        if (countdownTimer != null) countdownTimer.cancel();

        countdownTimer = new CountDownTimer(millisToCount, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long m = millisUntilFinished / 60000;
                long s = (millisUntilFinished % 60000) / 1000;
                tvCountdownTime.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));

                // Progress Bar tính theo thời gian còn lại thực tế
                progressBarNap.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                tvCountdownTime.setText("00:00");
                progressBarNap.setProgress(0);
                isRunning = false;

                // Hết giờ -> Xóa bộ nhớ
                prefs.edit().clear().apply();

                resetUIState();
            }
        }.start();
    }

    private void cancelAlarmAndTimer() {
        // Hủy báo thức
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NapReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) alarmManager.cancel(pendingIntent);

        // Hủy timer UI
        if (countdownTimer != null) countdownTimer.cancel();

        // QUAN TRỌNG: Xóa dữ liệu trong bộ nhớ
        prefs.edit().clear().apply();

        Toast.makeText(getContext(), "Đã dừng ngủ bù", Toast.LENGTH_SHORT).show();
        resetUIState();
    }

    private void resetUIState() {
        isRunning = false;
        containerSetup.setVisibility(View.VISIBLE);
        containerCountdown.setVisibility(View.GONE);

        btnStart.setText("Bắt đầu ngủ bù");
        btnStart.setIconResource(R.drawable.ic_bedtime);
        btnStart.setBackgroundColor(Color.parseColor("#6366f1"));

        updateUI(); // Cập nhật lại số phút mặc định
    }

    // ... (Giữ nguyên các hàm checkPermission, startRealAlarm, navigateBack, setupTimeControls...)

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                return false;
            }
        }
        return true;
    }

    private void startRealAlarm(int minutes) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NapReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Lỗi quyền báo thức", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
        else getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
    }

    private void initViews(View v) {
        tvMinutes = v.findViewById(R.id.tv_minutes);
        breathingGlow = v.findViewById(R.id.view_breathing_glow);
        btnStart = v.findViewById(R.id.btn_start_nap);
        btnWake = v.findViewById(R.id.btn_already_wake);
        containerSetup = v.findViewById(R.id.container_setup);
        containerCountdown = v.findViewById(R.id.container_countdown);
        tvCountdownTime = v.findViewById(R.id.tv_countdown_time);
        progressBarNap = v.findViewById(R.id.progress_bar_nap);

        quickButtons = new MaterialButton[]{
                v.findViewById(R.id.btn_15), v.findViewById(R.id.btn_25),
                v.findViewById(R.id.btn_30), v.findViewById(R.id.btn_60)
        };
        quickButtons[0].setTag(15); quickButtons[1].setTag(25);
        quickButtons[2].setTag(30); quickButtons[3].setTag(60);
    }

    private void setupTimeControls(View v) {
        v.findViewById(R.id.btn_minus).setOnClickListener(view -> changeTime(-5));
        v.findViewById(R.id.btn_plus).setOnClickListener(view -> changeTime(5));
        v.findViewById(R.id.btn_15).setOnClickListener(view -> setTime(15));
        v.findViewById(R.id.btn_25).setOnClickListener(view -> setTime(25));
        v.findViewById(R.id.btn_30).setOnClickListener(view -> setTime(30));
        v.findViewById(R.id.btn_60).setOnClickListener(view -> setTime(60));
    }
    private void changeTime(int delta) {
        int newTime = napMinutes + delta;
        if (newTime >= 5 && newTime <= 180) { napMinutes = newTime; updateUI(); }
    }
    private void setTime(int minutes) { napMinutes = minutes; updateUI(); }
    private void updateUI() {
        tvMinutes.setText(String.valueOf(napMinutes));
        for (MaterialButton btn : quickButtons) {
            int val = (int) btn.getTag();
            if (val == napMinutes) {
                btn.setBackgroundColor(Color.parseColor("#6366f1"));
                btn.setTextColor(Color.WHITE);
                btn.setStrokeWidth(0);
            } else {
                btn.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
                btn.setTextColor(Color.WHITE);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")));
                btn.setStrokeWidth(1);
            }
        }
    }
    private void startBreathingAnimation() {
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                breathingGlow,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f),
                PropertyValuesHolder.ofFloat("alpha", 0.5f)
        );
        scaleDown.setDuration(3000); scaleDown.setRepeatCount(ObjectAnimator.INFINITE); scaleDown.setRepeatMode(ObjectAnimator.REVERSE); scaleDown.setInterpolator(new AccelerateDecelerateInterpolator()); scaleDown.start();
    }
}