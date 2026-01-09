package com.example.moonpaw.fragment;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.moonpaw.R;
// import com.google.android.material.switchmaterial.SwitchMaterial; // Giao diện bạn của bạn không có Switch nên tạm bỏ

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    // Khai báo View theo ID của giao diện MỚI
    private TextView tvUsername, tvMusic, tvStreak, tvSleepTimeDisplay, tvDurationHint;
    private ImageView imgAvatar;

    // Các Layout bao bọc (để bắt sự kiện click)
    private LinearLayout cardProfileHeader, cardSleepSchedule, cardMusicStatus, btnUploadMusic, btnViewCalendar;

    private SharedPreferences sleepPrefs, userPrefs;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) saveImageToInternalStorage(uri); });

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) saveAudioToInternalStorage(uri); });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Đảm bảo file XML của bạn tên là fragment_profile và chứa code giao diện của bạn kia
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sleepPrefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
        userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        initViews(view);
        setupEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews(View v) {
        // Ánh xạ đúng ID trong XML của bạn kia
        tvUsername = v.findViewById(R.id.tv_username);
        imgAvatar = v.findViewById(R.id.img_avatar);
        tvStreak = v.findViewById(R.id.tv_streak_count);
        tvMusic = v.findViewById(R.id.tv_current_music);

        // Phần hiển thị giờ ngủ (Gộp chung)
        tvSleepTimeDisplay = v.findViewById(R.id.tv_sleep_time_display);
        tvDurationHint = v.findViewById(R.id.tv_sleep_duration_hint);

        // Các thẻ Card để bấm
        cardProfileHeader = v.findViewById(R.id.card_profile_header); // Thay cho btn_edit_name cũ
        cardSleepSchedule = v.findViewById(R.id.card_sleep_schedule); // Thay cho btn_pick_bedtime cũ
        cardMusicStatus = v.findViewById(R.id.card_music_status);
        btnUploadMusic = v.findViewById(R.id.btn_upload_music);
        btnViewCalendar = v.findViewById(R.id.btn_view_calendar);
    }

    private void loadData() {
        // 1. Tên & Avatar
        tvUsername.setText(userPrefs.getString("username", "Minh Anh"));
        File file = new File(requireContext().getFilesDir(), "profile_avatar.png");
        if (file.exists()) imgAvatar.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));

        // 2. Streak
        int streak = sleepPrefs.getInt("streak_count", 0);
        tvStreak.setText(streak + " ngày");

        // 3. Nhạc
        String playing = sleepPrefs.getString("current_playing_song", "");
        if(!playing.isEmpty()) tvMusic.setText(playing);
        else tvMusic.setText(sleepPrefs.getString("last_song_display", "Chưa nghe nhạc"));

        // 4. Giờ ngủ (Logic hiển thị kiểu mới: 22:00 -> 06:00)
        String bed = sleepPrefs.getString("bedtime", "22:00");
        String wake = sleepPrefs.getString("wakeup", "06:00");
        tvSleepTimeDisplay.setText(bed + " → " + wake);

        updateDurationHint(bed, wake);
    }

    private void setupEvents() {
        // Click vào Card Header để sửa tên (Logic cũ của bạn, áp vào nút mới)
        cardProfileHeader.setOnClickListener(v -> showEditNameDialog());

        // Click Avatar để đổi ảnh
        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Click Card Nhạc -> Chuyển màn hình
        cardMusicStatus.setOnClickListener(v -> {
            MusicFragment musicFragment = new MusicFragment();
            String songToPlay = sleepPrefs.getString("last_song_filename", "");
            if (!songToPlay.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("SONG_TO_PLAY", songToPlay);
                musicFragment.setArguments(args);
            }
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, musicFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Click Upload Nhạc
        btnUploadMusic.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chọn file MP3", Toast.LENGTH_SHORT).show();
            pickAudioLauncher.launch("audio/*");
        });

        // Click Xem Lịch
        btnViewCalendar.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CalendarFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Click Card Giờ ngủ -> Mở chọn giờ
        // Vì giao diện mới chỉ có 1 nút, ta sẽ cho chọn Giờ ngủ và tự động tính Giờ dậy (+8h)
        // Đây là cách trải nghiệm người dùng tốt nhất với giao diện này.
        cardSleepSchedule.setOnClickListener(v -> showSmartTimePicker());
    }

    // --- LOGIC CHỌN GIỜ (ĐÃ ĐIỀU CHỈNH CHO UI MỚI) ---
    private void showSmartTimePicker() {
        String currentBed = sleepPrefs.getString("bedtime", "22:00");
        String[] p = currentBed.split(":");

        new TimePickerDialog(getContext(), (v, h, m) -> {
            // 1. Lưu giờ ngủ mới
            String newBed = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            sleepPrefs.edit().putString("bedtime", newBed).apply();

            // 2. Tự động tính giờ dậy (+8 tiếng) - Giữ logic "isAuto8h" ngầm định
            int wakeH = (h + 8) % 24;
            String newWake = String.format(Locale.getDefault(), "%02d:%02d", wakeH, m);
            sleepPrefs.edit().putString("wakeup", newWake).apply();

            // 3. Cập nhật giao diện
            tvSleepTimeDisplay.setText(newBed + " → " + newWake);
            updateDurationHint(newBed, newWake);

            Toast.makeText(getContext(), "Đã đặt: Ngủ " + newBed + ", Dậy " + newWake, Toast.LENGTH_SHORT).show();

        }, Integer.parseInt(p[0]), Integer.parseInt(p[1]), true).show();
    }

    private void updateDurationHint(String bed, String wake) {
        try {
            String[] b = bed.split(":");
            String[] w = wake.split(":");
            int bm = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
            int wm = Integer.parseInt(w[0]) * 60 + Integer.parseInt(w[1]);
            int diff = wm - bm; if (diff < 0) diff += 1440;
            int h = diff / 60; int m = diff % 60;

            tvDurationHint.setText("Mục tiêu: 8 tiếng (Hiện tại: " + h + "h " + m + "p)");
            // Đổi màu chữ nếu ngủ ít quá
            tvDurationHint.setTextColor(android.graphics.Color.parseColor(h < 6 ? "#ef4444" : "#10b981"));
        } catch (Exception e) {}
    }

    // --- CÁC HÀM XỬ LÝ FILE (GIỮ NGUYÊN CỦA BẠN) ---

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final EditText input = new EditText(getContext());
        input.setText(tvUsername.getText());

        // Thêm margin cho đẹp
        LinearLayout container = new LinearLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 0, 50, 0);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setTitle("Đổi tên hiển thị");
        builder.setView(container);

        builder.setPositiveButton("Lưu", (d, w) -> {
            tvUsername.setText(input.getText());
            userPrefs.edit().putString("username", input.getText().toString()).apply();
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void saveAudioToInternalStorage(Uri uri) {
        try {
            String uniqueName = "song_" + System.currentTimeMillis() + ".mp3";
            String originalName = getFileName(uri);
            if(originalName == null) originalName = "Nhạc tải lên";

            File musicDir = new File(requireContext().getFilesDir(), "my_music");
            if (!musicDir.exists()) musicDir.mkdir();

            File destFile = new File(musicDir, uniqueName);
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
            is.close(); fos.close();

            sleepPrefs.edit()
                    .putString("last_song_filename", uniqueName)
                    .putString("last_song_display", originalName)
                    .apply();

            tvMusic.setText("Mới thêm: " + originalName);
            Toast.makeText(getContext(), "Đã lưu! Bấm vào thẻ để phát.", Toast.LENGTH_LONG).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgAvatar.setImageBitmap(bitmap);
            File file = new File(requireContext().getFilesDir(), "profile_avatar.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close(); is.close();
        } catch (Exception e) {}
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
}