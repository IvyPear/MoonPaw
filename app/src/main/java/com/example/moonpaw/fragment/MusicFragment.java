package com.example.moonpaw.fragment;

import android.app.AlertDialog;
import android.content.Context; // Thêm cái này
import android.content.SharedPreferences; // Thêm cái này
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicFragment extends Fragment {

    // BIẾN STATIC ĐỂ CHẠY NỀN
    private static MediaPlayer mediaPlayer;
    private static int currentSongIndex = -1;
    private static boolean isLoopOne = false;
    private static String currentSongTitle = "";

    private TextView tvSongName, tvStatus;
    private FloatingActionButton btnPlayPause;
    private ImageButton btnLoop, btnTimer;
    private List<Song> playlist;
    private SongAdapter adapter;
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
        setupPlaylist();
        updateUIIfPlaying();

        // Xử lý yêu cầu từ Profile
        if (getArguments() != null) {
            String songToPlay = getArguments().getString("SONG_TO_PLAY");
            if (songToPlay != null && !songToPlay.isEmpty()) {
                playSpecificSong(songToPlay);
                getArguments().remove("SONG_TO_PLAY");
            }
        }

        btnPlayPause.setOnClickListener(v -> {
            if (currentSongIndex == -1) {
                if (!playlist.isEmpty()) { currentSongIndex = 0; loadSong(currentSongIndex); playMusic(); }
            } else {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) pauseMusic(); else playMusic();
            }
        });
        view.findViewById(R.id.btn_next).setOnClickListener(v -> playNextSong());
        view.findViewById(R.id.btn_prev).setOnClickListener(v -> playPrevSong());

        if(isLoopOne) btnLoop.setColorFilter(android.graphics.Color.parseColor("#6366f1"));
        else btnLoop.setColorFilter(android.graphics.Color.parseColor("#64748b"));

        btnLoop.setOnClickListener(v -> {
            isLoopOne = !isLoopOne;
            if (isLoopOne) { btnLoop.setColorFilter(android.graphics.Color.parseColor("#6366f1")); if(mediaPlayer!=null) mediaPlayer.setLooping(true); }
            else { btnLoop.setColorFilter(android.graphics.Color.parseColor("#64748b")); if(mediaPlayer!=null) mediaPlayer.setLooping(false); }
        });
        btnTimer.setOnClickListener(v -> showTimerDialog());
    }

    private void updateUIIfPlaying() {
        if (mediaPlayer != null && currentSongIndex != -1) {
            tvSongName.setText(currentSongTitle);
            if (mediaPlayer.isPlaying()) {
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                tvStatus.setText("Đang phát");
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                tvStatus.setText("Tạm dừng");
            }
        }
    }

    private void setupPlaylist() {
        playlist.clear();
        playlist.add(new Song("Tiếng Mèo Gừ", "Thư giãn", "∞", R.raw.cat_purr));
        playlist.add(new Song("Mưa Rơi", "Thiên nhiên", "∞", R.raw.rain));
        playlist.add(new Song("Suối Chảy", "Thiên nhiên", "∞", R.raw.stream));
        playlist.add(new Song("Tiếng Ồn Trắng", "Tập trung", "∞", R.raw.white_noise));
        try {
            File musicDir = new File(requireContext().getFilesDir(), "my_music");
            if (musicDir.exists() && musicDir.isDirectory()) {
                File[] files = musicDir.listFiles();
                if (files != null) {
                    Arrays.sort(files);
                    for (File file : files) {
                        if (file.getName().endsWith(".mp3")) {
                            playlist.add(new Song(file.getName(), file.getAbsolutePath()));
                        }
                    }
                }
            }
        } catch (Exception e) {}
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void playSpecificSong(String fileName) {
        if (playlist == null || playlist.isEmpty()) return;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getTitle().equals(fileName)) {
                if (i == currentSongIndex && mediaPlayer != null && mediaPlayer.isPlaying()) return;
                currentSongIndex = i;
                loadSong(currentSongIndex);
                playMusic();
                return;
            }
        }
    }

    private void loadSong(int index) {
        if (playlist.isEmpty()) return;
        Song song = playlist.get(index);

        currentSongTitle = song.getTitle();
        tvSongName.setText(currentSongTitle);

        // --- QUAN TRỌNG: LƯU TÊN BÀI ĐANG HÁT ĐỂ PROFILE CẬP NHẬT ---
        SharedPreferences prefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("current_playing_song", currentSongTitle).apply();
        // -------------------------------------------------------------

        if (mediaPlayer != null) { mediaPlayer.release(); }

        try {
            mediaPlayer = new MediaPlayer();
            if (song.isCustom()) {
                mediaPlayer.setDataSource(song.getFilePath());
                mediaPlayer.prepare();
            } else {
                mediaPlayer = MediaPlayer.create(getContext(), song.getResourceId());
            }

            if (mediaPlayer != null) {
                if (isLoopOne) mediaPlayer.setLooping(true);
                else mediaPlayer.setOnCompletionListener(mp -> playNextSong());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void initViews(View v) {
        tvSongName = v.findViewById(R.id.tv_song_name);
        tvStatus = v.findViewById(R.id.tv_timer_status);
        btnPlayPause = v.findViewById(R.id.btn_play_pause);
        btnLoop = v.findViewById(R.id.btn_loop);
        btnTimer = v.findViewById(R.id.btn_sleep_timer);
        RecyclerView rvPlaylist = v.findViewById(R.id.rv_playlist);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));
        playlist = new ArrayList<>();
        adapter = new SongAdapter(playlist, song -> {
            currentSongIndex = playlist.indexOf(song);
            loadSong(currentSongIndex);
            playMusic();
        });
        rvPlaylist.setAdapter(adapter);
    }

    private void playMusic() { if(mediaPlayer!=null) {mediaPlayer.start(); btnPlayPause.setImageResource(R.drawable.ic_pause); tvStatus.setText("Đang phát");} }
    private void pauseMusic() { if(mediaPlayer!=null) {mediaPlayer.pause(); btnPlayPause.setImageResource(R.drawable.ic_play_arrow); tvStatus.setText("Tạm dừng");} }
    private void playNextSong() { if(playlist.isEmpty()) return; if(currentSongIndex < playlist.size()-1) currentSongIndex++; else currentSongIndex=0; loadSong(currentSongIndex); playMusic(); }
    private void playPrevSong() { if(playlist.isEmpty()) return; if(currentSongIndex > 0) currentSongIndex--; else currentSongIndex=playlist.size()-1; loadSong(currentSongIndex); playMusic(); }
    private void showTimerDialog() { String[] o = {"15p", "30p", "60p", "Hủy"}; new AlertDialog.Builder(getContext()).setItems(o, (d, w) -> { if(sleepTimer!=null) sleepTimer.cancel(); if(w==3){tvStatus.setText("Sẵn sàng");return;} startSleepTimer(w==0?15:(w==1?30:60)); }).show(); }
    private void startSleepTimer(int m) { sleepTimer = new CountDownTimer(m*60000L,1000){public void onTick(long l){tvStatus.setText("Tắt: "+l/60000+":"+(l%60000)/1000);} public void onFinish(){if(mediaPlayer!=null)pauseMusic();tvStatus.setText("Đã tắt");}}.start(); Toast.makeText(getContext(),"Tắt sau "+m+"p",Toast.LENGTH_SHORT).show(); }
    @Override public void onDestroy() { super.onDestroy(); if(sleepTimer!=null)sleepTimer.cancel(); }
}