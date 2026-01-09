package com.example.moonpaw.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

        // Cập nhật lại chuỗi khi quay lại (đề phòng vừa sửa bên Lịch)
        updateStreakLogic(System.currentTimeMillis());

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

        viewCircleBorder = v.findViewById(R.id.view_circle_border);

        // Setup click listener cho avatar (Logic tương tác vui nhộn)
        imgCatAvatar.setOnClickListener(view -> {
            boolean isSleeping = prefs.getLong("sleep_start", 0) > 0 && !prefs.getBoolean("cycle_completed", false);

            if (isSleeping) {
                Toast.makeText(getContext(), "Shh... Mèo đang ngủ 😴", Toast.LENGTH_SHORT).show();
                shakeAnimation(imgCatAvatar);
            } else {
                Toast.makeText(getContext(), "Meow! Chào bạn 😺", Toast.LENGTH_SHORT).show();
                bounceAnimation(imgCatAvatar);
            }
        });
    }

    private void setupListeners(View v) {
        // 1. Nút Bắt đầu ngủ
        btnStartSleep.setOnClickListener(view -> {
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putLong("sleep_start", now)
                    .putString("bedtime", SleepAnalyzer.formatTime(now))
                    .putBoolean("cycle_completed", false)
                    .apply();

            // Hiệu ứng chuyển sang ngủ
            animateTransition(() -> updateCatState(true, 0));

            refreshUI();
            Toast.makeText(getContext(), "Chúc bạn ngủ ngon! 🌙✨", Toast.LENGTH_SHORT).show();
        });

        // 2. Nút Tôi đã dậy
        btnWakeUp.setOnClickListener(view -> {
            long end = System.currentTimeMillis();
            long start = prefs.getLong("sleep_start", 0);

            if (start > 0) {
                float hours = SleepAnalyzer.calculateHours(start, end);
                String dateKey = SleepAnalyzer.getDateKey(start);

                // Lưu dữ liệu vào SharedPreferences
                prefs.edit()
                        .putFloat(dateKey, hours)
                        .putString("wakeup", SleepAnalyzer.formatTime(end))
                        .putLong("last_completion_time", end)
                        .putFloat("last_duration", hours)
                        .putBoolean("cycle_completed", true)
                        .remove("sleep_start")
                        .apply();

                // --- QUAN TRỌNG: Gọi hàm tính chuỗi SAU KHI LƯU ---
                // Để hàm này đọc được dữ liệu vừa lưu và đánh giá Good/Bad
                updateStreakLogic(end);
                // --------------------------------------------------

                // Hiệu ứng thức dậy
                animateTransition(() -> updateCatState(false, hours));

                refreshUI();
                Toast.makeText(getContext(), "Đã lưu " + String.format("%.1f", hours) + "h!", Toast.LENGTH_LONG).show();
            }
        });

        // 3. Các nút điều hướng
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

    /**
     * HÀM MỚI: Cập nhật UI theo Bảng đánh giá khoa học (3 Trục)
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
                imgRes = R.drawable.cat_awake;
                colorRes = SleepAnalyzer.COLOR_GOOD;
                statusText = "Lý tưởng";
                // Bảng 4: "Không vấn đề"
                tipText = "Giữ thói quen ngủ đều đặn giúp bạn duy trì năng lượng.";
                break;

            case OK:
                // Bao gồm: Thiếu nhẹ, Ngủ quá nhiều, Hoặc Ngủ đủ nhưng muộn
                imgRes = R.drawable.cat_yawning;
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
                    tipText = "Cơ thể chưa hồi phục hoàn toàn.";
                }
                break;

            case BAD:
                // Bao gồm: Thiếu nhiều (<6.5h)
                imgRes = R.drawable.cat_sleepy;
                colorRes = SleepAnalyzer.COLOR_BAD;
                statusText = "Cần chú ý";

                if (issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) {
                    tipText = "Ngủ muộn và thiếu giờ có thể gây mệt mỏi kéo dài.";
                } else {
                    // Mặc định cho SHORT_SLEEP
                    tipText = "Bạn nên ngủ thêm để cơ thể phục hồi tốt hơn.";
                }
                break;

            default:
                imgRes = R.drawable.cat_awake;
                colorRes = Color.GRAY;
                statusText = "--";
                tipText = "";
                break;
        }

        // Cập nhật Hình ảnh và Màu sắc lên UI
        if (imgCatAvatar != null) imgCatAvatar.setImageResource(imgRes);

        updateCircleColor(colorRes);

        if (tvSleepStatus != null) {
            tvSleepStatus.setText(statusText);
            updateBadgeColor(colorRes);
        }
        if (tvSleepTip != null) tvSleepTip.setText(tipText);
    }

    // --- CÁC HÀM VISUAL & ANIMATION ---

    private void updateCircleColor(int color) {
        if (viewCircleBg != null) {
            viewCircleBg.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (viewCircleBorder != null && viewCircleBorder.getBackground() != null) {
            viewCircleBorder.getBackground().setTint(color);
        }
    }

    private void updateBadgeColor(int color) {
        if (tvSleepStatus.getBackground() != null) {
            // Nếu là shape drawable
            tvSleepStatus.getBackground().setTint(color);
        }
    }

    // Animation chuyển đổi trạng thái mượt mà (Fade Out -> Update -> Fade In)
    private void animateTransition(Runnable updateAction) {
        if (imgCatAvatar == null) return;

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(250);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Cập nhật dữ liệu khi đã ẩn
                updateAction.run();

                // Hiện lại
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(250);
                imgCatAvatar.startAnimation(fadeIn);
            }
        });
        imgCatAvatar.startAnimation(fadeOut);
    }

    // Hiệu ứng Rung (Khi làm phiền mèo đang ngủ)
    private void shakeAnimation(View view) {
        view.animate().translationX(-10f).setDuration(50)
                .withEndAction(() -> view.animate().translationX(10f).setDuration(50)
                        .withEndAction(() -> view.animate().translationX(0f).setDuration(50).start()).start()).start();
    }

    // Hiệu ứng Nảy (Khi mèo vui)
    private void bounceAnimation(View view) {
        view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()).start();
    }

    // --- LOGIC HỆ THỐNG: TÍNH CHUỖI KHẮT KHE (CHỈ XANH/VÀNG MỚI ĐƯỢC TÍNH) ---
    private void updateStreakLogic(long currentTimeMs) {
        Calendar cal = Calendar.getInstance();
        int streak = 0;

        // 1. Kiểm tra ngày hôm nay
        String todayKey = SleepAnalyzer.getDateKey(cal.getTimeInMillis());
        float todayHours = prefs.getFloat(todayKey, 0f);

        if (todayHours > 0) {
            // Lấy giờ đi ngủ để đánh giá
            String tBed = prefs.getString("bedtime_" + todayKey, "23:00");
            SleepAnalyzer.SleepState state = SleepAnalyzer.evaluateSleepState(todayHours, false, tBed);

            if (state == SleepAnalyzer.SleepState.BAD) {
                // QUY TẮC MỚI: Nếu hôm nay ngủ "Đỏ" (Bad) -> MẤT CHUỖI NGAY LẬP TỨC
                prefs.edit().putInt("streak_count", 0).apply();
                return; // Dừng luôn, không cần quét quá khứ
            } else {
                // Xanh hoặc Vàng -> Được tính 1 điểm
                streak++;
            }
        }

        // 2. Quét ngược quá khứ
        cal.add(Calendar.DAY_OF_YEAR, -1); // Lùi về hôm qua

        while (true) {
            String key = SleepAnalyzer.getDateKey(cal.getTimeInMillis());
            float hours = prefs.getFloat(key, 0f);

            if (hours > 0) {
                String bedTime = prefs.getString("bedtime_" + key, "23:00");
                SleepAnalyzer.SleepState state = SleepAnalyzer.evaluateSleepState(hours, false, bedTime);

                if (state == SleepAnalyzer.SleepState.BAD) {
                    // QUY TẮC MỚI: Gặp ngày "Đỏ" trong quá khứ -> Dừng đếm tại đó
                    break;
                } else {
                    // Xanh hoặc Vàng -> Cộng tiếp
                    streak++;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
            } else {
                // Không có dữ liệu -> Dừng đếm
                break;
            }
        }

        // Lưu kết quả chuỗi
        prefs.edit().putInt("streak_count", streak).apply();
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
        // Hiển thị chuỗi từ prefs
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
            updateCatState(false, lastDuration); // Trạng thái: Đã hoàn thành

        } else if (isSleeping > 0) {
            updateButtonState(cardStartSleep, btnStartSleep, true);
            updateButtonState(cardWakeUp, btnWakeUp, false);

            updateCatState(true, 0); // Trạng thái: Đang ngủ

        } else {
            updateButtonState(cardStartSleep, btnStartSleep, false);
            updateButtonState(cardWakeUp, btnWakeUp, true);

            // Chế độ dự đoán (Prediction)
            float predictedHours = SleepAnalyzer.calculateDurationFromString(bedTimeStr, wakeUpStr);

            // Cập nhật mèo theo dự kiến (để người dùng biết trước)
            updateCatState(false, predictedHours);

            // Override text riêng cho trạng thái dự kiến
            tvSleepTip.setText("Dự kiến: " + String.format("%.1f", predictedHours) + "h (Nếu ngủ bây giờ)");
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