package com.example.moonpaw.fragment;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moonpaw.R;
import com.example.moonpaw.adapter.SongAdapter;
import com.example.moonpaw.model.Song;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {

    private MediaPlayer mediaPlayer;
    private TextView tvSongName, tvStatus;
    private FloatingActionButton btnPlayPause;
    private ImageButton btnLoop, btnTimer;

    private List<Song> playlist;
    private int currentSongIndex = -1;
    private boolean isLoopOne = false; // Mặc định: Lặp danh sách (khi hết bài này qua bài khác)
    private CountDownTimer sleepTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupPlaylist(); // Nạp dữ liệu các file mp3 thật

        // --- SỰ KIỆN NÚT BẤM ---

        // Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (currentSongIndex == -1) { // Chưa chọn bài nào -> Chơi bài đầu
                currentSongIndex = 0;
                loadSong(currentSongIndex);
                playMusic();
            } else {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    pauseMusic();
                } else {
                    playMusic();
                }
            }
        });

        // Next
        view.findViewById(R.id.btn_next).setOnClickListener(v -> playNextSong());

        // Prev
        view.findViewById(R.id.btn_prev).setOnClickListener(v -> playPrevSong());

        // Loop Mode
        btnLoop.setOnClickListener(v -> {
            isLoopOne = !isLoopOne;
            if (isLoopOne) {
                // Chế độ lặp 1 bài
                btnLoop.setColorFilter(android.graphics.Color.parseColor("#6366f1")); // Tím
                Toast.makeText(getContext(), "Chế độ: Lặp lại bài này", Toast.LENGTH_SHORT).show();
                if (mediaPlayer != null) mediaPlayer.setLooping(true);
            } else {
                // Chế độ phát tiếp
                btnLoop.setColorFilter(android.graphics.Color.parseColor("#64748b")); // Xám
                Toast.makeText(getContext(), "Chế độ: Phát theo danh sách", Toast.LENGTH_SHORT).show();
                if (mediaPlayer != null) mediaPlayer.setLooping(false);
            }
        });

        // Sleep Timer
        btnTimer.setOnClickListener(v -> showTimerDialog());
    }

    private void initViews(View v) {
        tvSongName = v.findViewById(R.id.tv_song_name);
        tvStatus = v.findViewById(R.id.tv_timer_status);
        btnPlayPause = v.findViewById(R.id.btn_play_pause);
        btnLoop = v.findViewById(R.id.btn_loop);
        btnTimer = v.findViewById(R.id.btn_sleep_timer);

        RecyclerView rvPlaylist = v.findViewById(R.id.rv_playlist);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo danh sách trống
        playlist = new ArrayList<>();

        // Adapter xử lý click vào bài hát
        SongAdapter adapter = new SongAdapter(playlist, song -> {
            currentSongIndex = playlist.indexOf(song);
            loadSong(currentSongIndex);
            playMusic();
        });
        rvPlaylist.setAdapter(adapter);
    }

    // Nạp file nhạc từ res/raw
    private void setupPlaylist() {
        playlist.clear();
        // ID trong R.raw.ten_file
        playlist.add(new Song("Tiếng Mèo Gừ", "Thư giãn", "∞", R.raw.cat_purr));
        playlist.add(new Song("Mưa Rơi", "Thiên nhiên", "∞", R.raw.rain));
        playlist.add(new Song("Suối Chảy", "Thiên nhiên", "∞", R.raw.stream));
        playlist.add(new Song("Tiếng Ồn Trắng", "Tập trung", "∞", R.raw.white_noise));
    }

    private void loadSong(int index) {
        if (playlist.isEmpty()) return;

        Song song = playlist.get(index);
        tvSongName.setText(song.getTitle());

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // Tạo MediaPlayer mới cho bài hát
        mediaPlayer = MediaPlayer.create(getContext(), song.getResourceId());

        // Cài đặt Loop theo trạng thái hiện tại
        if (isLoopOne) {
            mediaPlayer.setLooping(true);
        } else {
            mediaPlayer.setLooping(false);
            // Nếu không lặp 1 bài -> Hết bài sẽ qua bài kế
            mediaPlayer.setOnCompletionListener(mp -> playNextSong());
        }
    }

    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            tvStatus.setText("Đang phát");
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            tvStatus.setText("Tạm dừng");
        }
    }

    private void playNextSong() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
        } else {
            currentSongIndex = 0; // Hết danh sách -> Quay về đầu
        }
        loadSong(currentSongIndex);
        playMusic();
    }

    private void playPrevSong() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else {
            currentSongIndex = playlist.size() - 1;
        }
        loadSong(currentSongIndex);
        playMusic();
    }

    private void showTimerDialog() {
        String[] options = {"15 phút", "30 phút", "45 phút", "60 phút", "Hủy hẹn giờ"};

        new AlertDialog.Builder(getContext())
                .setTitle("Hẹn giờ tắt nhạc")
                .setItems(options, (dialog, which) -> {
                    if (sleepTimer != null) sleepTimer.cancel();

                    int minutes = 0;
                    switch (which) {
                        case 0: minutes = 15; break;
                        case 1: minutes = 30; break;
                        case 2: minutes = 45; break;
                        case 3: minutes = 60; break;
                        case 4:
                            tvStatus.setText(mediaPlayer != null && mediaPlayer.isPlaying() ? "Đang phát" : "Sẵn sàng");
                            Toast.makeText(getContext(), "Đã hủy hẹn giờ", Toast.LENGTH_SHORT).show();
                            return;
                    }
                    startSleepTimer(minutes);
                })
                .show();
    }

    private void startSleepTimer(int minutes) {
        long millis = minutes * 60 * 1000;

        sleepTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long m = millisUntilFinished / 60000;
                long s = (millisUntilFinished % 60000) / 1000;
                tvStatus.setText(String.format("Tắt sau: %02d:%02d", m, s));
            }

            @Override
            public void onFinish() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    pauseMusic();
                }
                tvStatus.setText("Đã tắt nhạc");
                Toast.makeText(getContext(), "Đã tắt nhạc theo hẹn giờ", Toast.LENGTH_LONG).show();
            }
        }.start();

        Toast.makeText(getContext(), "Nhạc sẽ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Giải phóng tài nguyên khi thoát màn hình
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (sleepTimer != null) sleepTimer.cancel();
    }
}