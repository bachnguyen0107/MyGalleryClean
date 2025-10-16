package com.example.midterm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private Context context;
    private List<Uri> imageUris;
    private Map<Uri, String> imageTags = new HashMap<>();
    private static final String TAG = "ImageAdapter";

    public ImageAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris != null ? imageUris : new ArrayList<>();

    }

    public void loadTagsForImages() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                PhotoDao photoDao = db.photoDao();
                Map<Uri, String> newImageTags = new HashMap<>();

                for (Uri uri : imageUris) {
                    try {
                        Photo photo = photoDao.getPhotoByUri(uri.toString());
                        if (photo != null && photo.tag != null && !photo.tag.isEmpty()) {
                            newImageTags.put(uri, photo.tag);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading tags for URI: " + uri, e);
                    }
                }

                imageTags.clear();
                imageTags.putAll(newImageTags);
                notifyDataSetChanged();

            } catch (Exception e) {
                Log.e(TAG, "Error loading tags", e);
            }
        }).start();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Uri imageUri = imageUris.get(position);

            // Load image with Glide
            Glide.with(context)
                    .load(imageUri)
                    .into(holder.imageView);

            // Set tag text
            String tags = imageTags.get(imageUri);
            if (tags != null && !tags.isEmpty()) {
                String displayTags = formatTagsForDisplay(tags);
                holder.textViewTag.setText(displayTags);
                holder.textViewTag.setVisibility(View.VISIBLE);
            } else {
                holder.textViewTag.setVisibility(View.GONE);
            }

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenActivity.class);
                intent.putExtra("imageUri", imageUri.toString());
                context.startActivity(intent);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder", e);
        }
    }

    private String formatTagsForDisplay(String tags) {
        if (tags == null || tags.isEmpty()) return "";

        try {
            String[] tagArray = tags.split(",");
            if (tagArray.length == 0) return "";

            String firstTag = tagArray[0].trim();
            if (tagArray.length > 1) {
                return firstTag + " (+" + (tagArray.length - 1) + ")";
            }
            return firstTag;
        } catch (Exception e) {
            return tags;
        }
    }

    @Override
    public int getItemCount() {
        return imageUris != null ? imageUris.size() : 0;
    }

    public void updateData(List<Uri> newUris) {
        this.imageUris = newUris != null ? newUris : new ArrayList<>();
        this.imageTags.clear();
        loadTagsForImages();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewTag;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            textViewTag = itemView.findViewById(R.id.textViewTag);
        }
    }
}