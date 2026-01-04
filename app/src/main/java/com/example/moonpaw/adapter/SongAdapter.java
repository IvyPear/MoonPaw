package com.example.moonpaw.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.moonpaw.R;
import com.example.moonpaw.model.Song;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(List<Song> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.title.setText(song.getTitle());
        holder.category.setText(song.getCategory());
        holder.duration.setText(song.getDuration());

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, duration;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            category = itemView.findViewById(R.id.tv_category);
            duration = itemView.findViewById(R.id.tv_duration);
        }
    }
}