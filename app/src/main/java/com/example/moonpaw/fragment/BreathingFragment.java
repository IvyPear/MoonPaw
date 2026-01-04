package com.example.moonpaw.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;

import java.util.Locale;

public class BreathingFragment extends Fragment {

    private View circleCenter, circleOuter;
    private TextView tvStatus, tvGuide, tvTimer;
    private ProgressBar progressBar;
    private ImageButton btnPlayPause;

    private CountDownTimer sessionTimer;
    private Handler breathHandler = new Handler();
    private boolean isPlaying = false;
    private long timeLeftInMillis = 180000; // 3 phút
    private final long TOTAL_TIME = 180000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_breathing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        // Tự động bắt đầu
        startSession();

        // Nút Back
        view.findViewById(R.id.btn_close).setOnClickListener(v -> requireActivity().onBackPressed());

        // Nút Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) pauseSession();
            else startSession();
        });

        // Nút Replay
        view.findViewById(R.id.btn_replay).setOnClickListener(v -> {
            resetSession();
        });
    }

    private void initViews(View v) {
        circleCenter = v.findViewById(R.id.circle_center);
        circleOuter = v.findViewById(R.id.circle_outer_glow);
        tvStatus = v.findViewById(R.id.tv_breath_status);
        tvGuide = v.findViewById(R.id.tv_breath_guide);
        tvTimer = v.findViewById(R.id.tv_timer);
        progressBar = v.findViewById(R.id.progress_breath);
        btnPlayPause = v.findViewById(R.id.btn_play_pause);
    }

    private void startSession() {
        isPlaying = true;
        btnPlayPause.setImageResource(R.drawable.ic_pause);

        // Bắt đầu đếm ngược thời gian tổng
        sessionTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                finishSession();
            }
        }.start();

        // Bắt đầu chu kỳ thở (Hít - Giữ - Thở)
        startBreathCycle();
    }

    private void pauseSession() {
        isPlaying = false;
        btnPlayPause.setImageResource(R.drawable.ic_play_arrow); // Nhớ tạo icon play

        if (sessionTimer != null) sessionTimer.cancel();
        breathHandler.removeCallbacksAndMessages(null);
        stopAnimation();
    }

    private void resetSession() {
        pauseSession();
        timeLeftInMillis = TOTAL_TIME;
        updateTimerUI();
        startSession();
    }

    private void finishSession() {
        isPlaying = false;
        tvStatus.setText("Hoàn thành");
        tvGuide.setText("Bạn làm tốt lắm!");
        btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
        stopAnimation();
    }

    // --- LOGIC NHỊP THỞ (4-7-8 hoặc 4-4-4 cơ bản) ---
    private void startBreathCycle() {
        if (!isPlaying) return;

        // 1. HÍT VÀO (4 giây)
        tvStatus.setText("Hít vào");
        tvGuide.setText("Hít sâu bằng mũi...");
        animateCircle(1.0f, 1.3f, 4000); // Phóng to

        breathHandler.postDelayed(() -> {
            if (!isPlaying) return;

            // 2. GIỮ HƠI (4 giây)
            tvStatus.setText("Giữ hơi");
            tvGuide.setText("Thả lỏng cơ thể...");
            // Giữ nguyên kích thước (không scale thêm)

            breathHandler.postDelayed(() -> {
                if (!isPlaying) return;

                // 3. THỞ RA (4 giây)
                tvStatus.setText("Thở ra");
                tvGuide.setText("Thở chậm qua miệng...");
                animateCircle(1.3f, 1.0f, 4000); // Thu nhỏ

                // Lặp lại chu kỳ
                breathHandler.postDelayed(this::startBreathCycle, 4000);

            }, 4000); // Hết giữ hơi

        }, 4000); // Hết hít vào
    }

    private void animateCircle(float fromScale, float toScale, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(circleCenter, "scaleX", fromScale, toScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(circleCenter, "scaleY", fromScale, toScale);

        // Hiệu ứng vòng ngoài mờ ảo
        ObjectAnimator glowX = ObjectAnimator.ofFloat(circleOuter, "scaleX", fromScale, toScale * 1.2f);
        ObjectAnimator glowY = ObjectAnimator.ofFloat(circleOuter, "scaleY", fromScale, toScale * 1.2f);
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(circleOuter, "alpha", 0.1f, 0.4f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, glowX, glowY, glowAlpha);
        set.setDuration(duration);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void stopAnimation() {
        circleCenter.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
        circleOuter.animate().scaleX(1f).scaleY(1f).alpha(0.1f).setDuration(500).start();
    }

    private void updateTimerUI() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((timeLeftInMillis * 100) / TOTAL_TIME);
        progressBar.setProgress(progress);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pauseSession(); // Dừng mọi thứ khi thoát
    }
}