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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Uri> imageUris;
    private Map<Uri, String> imageTags = new HashMap<>();
    private static final String TAG = "ImageAdapter";

    // View types
    public static final int VIEW_TYPE_GRID = 0;
    public static final int VIEW_TYPE_LIST = 1;

    private int currentViewType = VIEW_TYPE_GRID; // Default to grid view

    public ImageAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris != null ? imageUris : new ArrayList<>();
    }

    public void setViewType(int viewType) {
        this.currentViewType = viewType;
        notifyDataSetChanged();
    }

    public int getViewType() {
        return currentViewType;
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

    @Override
    public int getItemViewType(int position) {
        return currentViewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_LIST) {
            View view = inflater.inflate(R.layout.item_image, parent, false); // list view layout
            return new ListViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_image_grid, parent, false); // Grid view layout
            return new GridViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            Uri imageUri = imageUris.get(position);

            if (holder instanceof GridViewHolder) {
                bindGridViewHolder((GridViewHolder) holder, imageUri);
            } else if (holder instanceof ListViewHolder) {
                bindListViewHolder((ListViewHolder) holder, imageUri);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder", e);
        }
    }

    private void bindGridViewHolder(GridViewHolder holder, Uri imageUri) {
        // Load image
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

        holder.itemView.setOnClickListener(v -> {
            openFullScreenActivity(imageUri);
        });
    }

    private void bindListViewHolder(ListViewHolder holder, Uri imageUri) {
        // Load image
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // Set file name
        String fileName = getFileNameFromUri(imageUri);
        holder.textViewFileName.setText(fileName);

        // Set tags
        String tags = imageTags.get(imageUri);
        if (tags != null && !tags.isEmpty()) {
            holder.textViewTag.setText("Tags: " + tags);
            holder.textViewTag.setVisibility(View.VISIBLE);
        } else {
            holder.textViewTag.setText("No tags");
            holder.textViewTag.setVisibility(View.VISIBLE);
        }

        // Set date (using current time as placeholder)
        String dateString = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        holder.textViewDate.setText(dateString);

        holder.itemView.setOnClickListener(v -> {
            openFullScreenActivity(imageUri);
        });
    }

    private String getFileNameFromUri(Uri uri) {
        try {
            String path = uri.getLastPathSegment();
            if (path != null && path.contains("/")) {
                return path.substring(path.lastIndexOf("/") + 1);
            }
            return path != null ? path : "Image";
        } catch (Exception e) {
            return "Image";
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

    private void openFullScreenActivity(Uri imageUri) {
        Intent intent = new Intent(context, FullScreenActivity.class);
        intent.putExtra("imageUri", imageUri.toString());
        context.startActivity(intent);
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

    // Grid View Holder (uses item_image_grid.xml)
    public static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewTag;

        public GridViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            textViewTag = itemView.findViewById(R.id.textViewTag);
        }
    }

    // List View Holder (uses item_image.xml)
    public static class ListViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewTag;
        TextView textViewDate;

        public ListViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            textViewTag = itemView.findViewById(R.id.textViewTag);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }
}