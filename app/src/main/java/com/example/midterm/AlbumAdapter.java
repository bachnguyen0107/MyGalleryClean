package com.example.midterm;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private final Context context;
    private final List<Album> albums;

    public AlbumAdapter(Context context, List<Album> albums) {
        this.context = context;
        this.albums = albums;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumName.setText(album.name);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AlbumDetailActivity.class);
            intent.putExtra("albumId", album.id);
            intent.putExtra("albumName", album.name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView albumName;
        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.textAlbumName);
        }
    }
}
