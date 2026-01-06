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
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvMusic, tvBedtime, tvWakeup, tvStreak, tvDurationHint;
    private ImageView imgAvatar;
    private SwitchMaterial switchAuto8h, switchNotification, switchTheme;
    private LinearLayout cardMusicStatus, btnUploadMusic;
    private SharedPreferences sleepPrefs, userPrefs;
    private boolean isAuto8h = true;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) saveImageToInternalStorage(uri); });

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) saveAudioToInternalStorage(uri); });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Khởi tạo SharedPreferences
        sleepPrefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
        userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        initViews(view);
        setupEvents();

        // Lưu ý: loadData() sẽ được gọi trong onResume() để luôn cập nhật mới nhất
    }

    // --- QUAN TRỌNG: CẬP NHẬT DỮ LIỆU MỖI KHI VÀO MÀN HÌNH NÀY ---
    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Gọi hàm load lại toàn bộ dữ liệu (Streak, Nhạc, Giờ...)
    }

    private void loadData() {
        // 1. Tên người dùng
        tvUsername.setText(userPrefs.getString("username", "Minh Anh"));

        // 2. Avatar
        File file = new File(requireContext().getFilesDir(), "profile_avatar.png");
        if (file.exists()) imgAvatar.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));

        // 3. Giờ giấc
        tvBedtime.setText(sleepPrefs.getString("bedtime", "22:00"));
        tvWakeup.setText(sleepPrefs.getString("wakeup", "06:00"));

        // 4. Cài đặt Switch
        isAuto8h = userPrefs.getBoolean("auto_8h", true);
        switchAuto8h.setChecked(isAuto8h);
        switchNotification.setChecked(userPrefs.getBoolean("notifications_enabled", true));

        // --- 5. SỬA LỖI STREAK Ở ĐÂY ---
        // Dùng key "streak_count" cho khớp với bên HomeFragment
        int streak = sleepPrefs.getInt("streak_count", 0);
        tvStreak.setText(streak + " ngày");

        // 6. Trạng thái nhạc
        String playing = sleepPrefs.getString("current_playing_song", "");
        if(!playing.isEmpty()) tvMusic.setText(playing);
        else tvMusic.setText(sleepPrefs.getString("last_song_display", "Chưa nghe nhạc"));

        updateDurationHint();
    }

    private void initViews(View v) {
        tvUsername = v.findViewById(R.id.tv_username);
        imgAvatar = v.findViewById(R.id.img_avatar);
        tvStreak = v.findViewById(R.id.tv_streak_count);
        tvMusic = v.findViewById(R.id.tv_current_music);
        tvBedtime = v.findViewById(R.id.tv_bedtime);
        tvWakeup = v.findViewById(R.id.tv_wakeup);
        tvDurationHint = v.findViewById(R.id.tv_sleep_duration_hint);

        cardMusicStatus = v.findViewById(R.id.card_music_status);
        btnUploadMusic = v.findViewById(R.id.btn_upload_music);

        switchTheme = v.findViewById(R.id.switch_theme);
        switchAuto8h = v.findViewById(R.id.switch_auto_8h);
        switchNotification = v.findViewById(R.id.switch_notification);
    }

    private void setupEvents() {
        getView().findViewById(R.id.btn_edit_name).setOnClickListener(v -> showEditNameDialog());
        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Chuyển sang Music và phát nhạc
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

        btnUploadMusic.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chọn file MP3", Toast.LENGTH_SHORT).show();
            pickAudioLauncher.launch("audio/*");
        });

        // Switch Logic
        switchTheme.setOnCheckedChangeListener((v, c) -> {
            if(c) {
                Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
                v.postDelayed(() -> v.setChecked(false), 500);
            }
        });

        switchAuto8h.setOnCheckedChangeListener((v, c) -> {
            isAuto8h = c;
            userPrefs.edit().putBoolean("auto_8h", c).apply();
            if (c) calculateWakeupFromBedtime();
        });

        switchNotification.setOnCheckedChangeListener((v, c) ->
                userPrefs.edit().putBoolean("notifications_enabled", c).apply()
        );

        // Time Picker Logic
        getView().findViewById(R.id.btn_pick_bedtime).setOnClickListener(v -> showTimePicker(true));
        getView().findViewById(R.id.btn_pick_wakeup).setOnClickListener(v -> {
            if (isAuto8h) Toast.makeText(getContext(), "Tắt tự động để chỉnh!", Toast.LENGTH_SHORT).show();
            else showTimePicker(false);
        });
    }

    // --- LOGIC FILE & TIỆN ÍCH ---
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
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void saveImageToInternalStorage(Uri uri) { try { InputStream is = requireContext().getContentResolver().openInputStream(uri); Bitmap bitmap = BitmapFactory.decodeStream(is); imgAvatar.setImageBitmap(bitmap); File file = new File(requireContext().getFilesDir(), "profile_avatar.png"); FileOutputStream fos = new FileOutputStream(file); bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close(); is.close(); } catch (Exception e) {} }
    private void showEditNameDialog() { AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); final EditText input = new EditText(getContext()); input.setText(tvUsername.getText()); builder.setView(input).setPositiveButton("Lưu", (d, w) -> { tvUsername.setText(input.getText()); userPrefs.edit().putString("username", input.getText().toString()).apply(); }).show(); }

    private void showTimePicker(boolean isBedtime) {
        String[] p = (isBedtime ? tvBedtime : tvWakeup).getText().toString().split(":");
        new TimePickerDialog(getContext(), (v, h, m) -> {
            String t = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            if(isBedtime) {
                tvBedtime.setText(t);
                sleepPrefs.edit().putString("bedtime", t).apply();
                if(isAuto8h) calculateWakeupFromBedtime();
                else updateDurationHint();
            } else {
                tvWakeup.setText(t);
                sleepPrefs.edit().putString("wakeup", t).apply();
                updateDurationHint();
            }
        }, Integer.parseInt(p[0]), Integer.parseInt(p[1]), true).show();
    }

    private void calculateWakeupFromBedtime() {
        String[] p = tvBedtime.getText().toString().split(":");
        tvWakeup.setText(String.format(Locale.getDefault(), "%02d:%02d", (Integer.parseInt(p[0]) + 8) % 24, Integer.parseInt(p[1])));
        sleepPrefs.edit().putString("wakeup", tvWakeup.getText().toString()).apply();
        updateDurationHint();
    }

    private void updateDurationHint() {
        try {
            String[] b = tvBedtime.getText().toString().split(":");
            String[] w = tvWakeup.getText().toString().split(":");
            int bm = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
            int wm = Integer.parseInt(w[0]) * 60 + Integer.parseInt(w[1]);
            int diff = wm - bm; if (diff < 0) diff += 1440; int h = diff / 60; int m = diff % 60;
            tvDurationHint.setText("Mục tiêu: 8 tiếng (Hiện tại: " + h + "h " + m + "p)");
            tvDurationHint.setTextColor(android.graphics.Color.parseColor(h < 6 ? "#ef4444" : "#10b981"));
        } catch (Exception e) {}
    }
}