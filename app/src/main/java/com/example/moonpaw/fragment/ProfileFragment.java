package com.example.moonpaw.fragment;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout; // Nhớ import cái này
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    // Khai báo biến View
    private TextView tvUsername, tvStreak, tvMusic, tvBedtime, tvWakeup, tvDurationHint;
    private ImageView imgAvatar;
    private SwitchMaterial switchAuto8h, switchNotification;
    private LinearLayout cardMusicStatus; // Biến cho thẻ nhạc

    // SharedPreferences
    private SharedPreferences sleepPrefs;
    private SharedPreferences userPrefs;

    private boolean isAuto8h = true;

    // --- BỘ CHỌN ẢNH TỪ THƯ VIỆN (Launcher) ---
    // Thay thế cho mảng icon cũ
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Khi người dùng chọn ảnh xong -> Lưu và Hiển thị
                    saveImageToInternalStorage(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sleepPrefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
        userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        initViews(view);
        loadData();

        // --- XỬ LÝ SỰ KIỆN ---

        // 1. Đổi tên
        view.findViewById(R.id.btn_edit_name).setOnClickListener(v -> showEditNameDialog());

        // 2. Đổi Avatar -> MỞ THƯ VIỆN ẢNH
        view.findViewById(R.id.img_avatar).setOnClickListener(v -> {
            // Mở thư viện, chỉ lọc lấy file ảnh
            pickImageLauncher.launch("image/*");
        });

        // 3. Click Thẻ Nhạc -> CHUYỂN SANG MUSIC FRAGMENT
        cardMusicStatus.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MusicFragment())
                    .addToBackStack(null) // Để bấm Back quay lại được đây
                    .commit();
        });

        // 4. Switch: Tự động 8 tiếng
        switchAuto8h.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAuto8h = isChecked;
            userPrefs.edit().putBoolean("auto_8h", isChecked).apply();
            if (isChecked) calculateWakeupFromBedtime();
            else Toast.makeText(getContext(), "Đã chuyển sang chế độ thủ công", Toast.LENGTH_SHORT).show();
        });

        // 5. Switch: Thông báo
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userPrefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "Đã BẬT nhắc nhở" : "Đã TẮT nhắc nhở", Toast.LENGTH_SHORT).show();
        });

        // 6. Chọn giờ
        view.findViewById(R.id.btn_pick_bedtime).setOnClickListener(v -> showTimePicker(true));
        view.findViewById(R.id.btn_pick_wakeup).setOnClickListener(v -> {
            if (isAuto8h) Toast.makeText(getContext(), "Tắt chế độ 'Tự động 8h' để chỉnh giờ dậy!", Toast.LENGTH_SHORT).show();
            else showTimePicker(false);
        });
    }

    private void initViews(View v) {
        tvUsername = v.findViewById(R.id.tv_username);
        imgAvatar = v.findViewById(R.id.img_avatar);
        tvStreak = v.findViewById(R.id.tv_streak_count);
        tvMusic = v.findViewById(R.id.tv_current_music);
        tvBedtime = v.findViewById(R.id.tv_bedtime);
        tvWakeup = v.findViewById(R.id.tv_wakeup);
        tvDurationHint = v.findViewById(R.id.tv_sleep_duration_hint);

        // ID này phải khớp với XML (LinearLayout chứa nhạc)
        cardMusicStatus = v.findViewById(R.id.card_music_status);

        switchAuto8h = v.findViewById(R.id.switch_auto_8h);
        switchNotification = v.findViewById(R.id.switch_notification);
    }

    private void loadData() {
        // Load Tên
        tvUsername.setText(userPrefs.getString("username", "Minh Anh"));

        // --- LOAD ẢNH ĐẠI DIỆN ---
        // Kiểm tra xem đã có ảnh lưu trong máy chưa
        File file = new File(requireContext().getFilesDir(), "profile_avatar.png");
        if (file.exists()) {
            // Nếu có -> Load ảnh từ file lên
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            imgAvatar.setImageBitmap(bitmap);
        } else {
            // Nếu chưa -> Dùng ảnh mặc định con mèo
            imgAvatar.setImageResource(R.drawable.ic_cat_breathing);
        }

        // Load Settings
        isAuto8h = userPrefs.getBoolean("auto_8h", true);
        switchAuto8h.setChecked(isAuto8h);
        switchNotification.setChecked(userPrefs.getBoolean("notifications_enabled", true));

        // Load Giờ
        String bed = sleepPrefs.getString("bedtime", "22:00");
        String wake = sleepPrefs.getString("wakeup", "06:00");
        tvBedtime.setText(bed);
        tvWakeup.setText(wake);

        // Load Stats
        int streak = sleepPrefs.getInt("current_streak", 0);
        tvStreak.setText(streak + " ngày");
        String lastSong = sleepPrefs.getString("last_song_name", "Chưa nghe nhạc");
        tvMusic.setText(lastSong);

        updateDurationHint();
    }

    // --- HÀM LƯU ẢNH VÀO BỘ NHỚ TRONG ---
    // (Giúp ảnh không bị mất khi tắt app)
    private void saveImageToInternalStorage(Uri uri) {
        try {
            // 1. Đọc ảnh từ URI
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // 2. Hiển thị ngay lên ImageView
            imgAvatar.setImageBitmap(bitmap);

            // 3. Lưu thành file "profile_avatar.png" trong thư mục riêng của app
            File file = new File(requireContext().getFilesDir(), "profile_avatar.png");
            FileOutputStream fos = new FileOutputStream(file);

            // Nén ảnh (giảm dung lượng chút cho nhẹ app)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            inputStream.close();

            Toast.makeText(getContext(), "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- LOGIC ĐỔI TÊN ---
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Đổi tên hiển thị");
        final EditText input = new EditText(getContext());
        input.setText(tvUsername.getText());
        input.setSelection(input.getText().length());
        builder.setView(input);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                tvUsername.setText(newName);
                userPrefs.edit().putString("username", newName).apply();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- LOGIC GIỜ GIẤC (Giữ nguyên) ---
    private void showTimePicker(boolean isBedtime) {
        String current = isBedtime ? tvBedtime.getText().toString() : tvWakeup.getText().toString();
        String[] parts = current.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        TimePickerDialog picker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (isBedtime) {
                tvBedtime.setText(time);
                sleepPrefs.edit().putString("bedtime", time).apply();
                if (isAuto8h) calculateWakeupFromBedtime();
                else validateDuration();
            } else {
                tvWakeup.setText(time);
                if (validateDuration()) sleepPrefs.edit().putString("wakeup", time).apply();
            }
        }, h, m, true);
        picker.show();
    }

    private void calculateWakeupFromBedtime() {
        String[] parts = tvBedtime.getText().toString().split(":");
        int bedH = Integer.parseInt(parts[0]);
        int bedM = Integer.parseInt(parts[1]);
        int wakeH = (bedH + 8) % 24;
        String wakeTime = String.format(Locale.getDefault(), "%02d:%02d", wakeH, bedM);
        tvWakeup.setText(wakeTime);
        sleepPrefs.edit().putString("wakeup", wakeTime).apply();
        updateDurationHint();
    }

    private boolean validateDuration() {
        int bedMins = timeToMins(tvBedtime.getText().toString());
        int wakeMins = timeToMins(tvWakeup.getText().toString());
        int diff = wakeMins - bedMins;
        if (diff < 0) diff += 1440;

        if (diff < 360) {
            tvDurationHint.setText("⚠️ Cảnh báo: Ngủ dưới 6 tiếng không tốt!");
            tvDurationHint.setTextColor(Color.parseColor("#ef4444"));
            Toast.makeText(getContext(), "Bạn nên ngủ ít nhất 6 tiếng!", Toast.LENGTH_LONG).show();
            return false;
        } else {
            updateDurationHint();
            return true;
        }
    }

    private void updateDurationHint() {
        int bedMins = timeToMins(tvBedtime.getText().toString());
        int wakeMins = timeToMins(tvWakeup.getText().toString());
        int diff = wakeMins - bedMins;
        if (diff < 0) diff += 1440;
        int h = diff / 60;
        int m = diff % 60;
        tvDurationHint.setText("Mục tiêu: " + h + " tiếng " + (m > 0 ? m + " phút" : ""));
        tvDurationHint.setTextColor(Color.parseColor("#10b981"));
    }

    private int timeToMins(String time) {
        try {
            String[] p = time.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) { return 0; }
    }
}