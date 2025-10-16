package com.example.midterm;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullScreenActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button btnAddTag;
    private LinearLayout tagsContainer;
    private Uri currentUri;
    private PhotoDao photoDao;
    private ExecutorService executorService;
    private Photo currentPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        imageView = findViewById(R.id.imageViewFullScreen);
        btnAddTag = findViewById(R.id.btnAddTag);
        tagsContainer = findViewById(R.id.tagsContainer);

        // Initialize database
        AppDatabase db = AppDatabase.getDatabase(this);
        photoDao = db.photoDao();
        executorService = Executors.newSingleThreadExecutor();

        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString != null) {
            currentUri = Uri.parse(uriString);
            Glide.with(this)
                    .load(currentUri)
                    .into(imageView);
            loadPhotoTags();
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnAddTag.setOnClickListener(v -> showAddTagDialog());
    }

    private void loadPhotoTags() {
        executorService.execute(() -> {
            // Try to find existing photo in database
            Photo photo = photoDao.getPhotoByUri(currentUri.toString());
            runOnUiThread(() -> {
                if (photo != null) {
                    currentPhoto = photo;
                    displayTags(photo.tag);
                } else {
                    // Create new photo entry if it doesn't exist
                    currentPhoto = new Photo(currentUri.toString(), 0, ""); // albumId 0 for "All Photos"
                    executorService.execute(() -> {
                        photoDao.insert(currentPhoto);
                    });
                    displayTags("");
                }
            });
        });
    }

    private void displayTags(String tags) {
        tagsContainer.removeAllViews();

        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    addTagView(trimmedTag);
                }
            }
        }

        // Show message if no tags
        if (tagsContainer.getChildCount() == 0) {
            TextView noTagsText = new TextView(this);
            noTagsText.setText("No tags added");
            noTagsText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noTagsText.setTextSize(14);
            noTagsText.setPadding(16, 16, 16, 16);
            tagsContainer.addView(noTagsText);
        }
    }

    private void addTagView(String tag) {
        View tagView = getLayoutInflater().inflate(R.layout.item_tag, tagsContainer, false);
        TextView tagText = tagView.findViewById(R.id.textTag);
        ImageView btnRemove = tagView.findViewById(R.id.btnRemoveTag);

        tagText.setText(tag);

        btnRemove.setOnClickListener(v -> {
            removeTag(tag);
            tagsContainer.removeView(tagView);
            // Refresh the display in case this was the last tag
            if (tagsContainer.getChildCount() == 1) { // Only the "no tags" message remains
                displayTags("");
            }
        });

        tagsContainer.addView(tagView);
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        final EditText input = new EditText(this);
        input.setHint("Enter tag name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String tag = input.getText().toString().trim();
            if (!tag.isEmpty()) {
                addNewTag(tag);
            } else {
                Toast.makeText(this, "Tag cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addNewTag(String newTag) {
        executorService.execute(() -> {
            String currentTags = currentPhoto.tag;
            String updatedTags;

            if (currentTags == null || currentTags.isEmpty()) {
                updatedTags = newTag;
            } else {
                // Check if tag already exists
                String[] existingTags = currentTags.split(",");
                for (String existingTag : existingTags) {
                    if (existingTag.trim().equalsIgnoreCase(newTag)) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Tag already exists", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }
                updatedTags = currentTags + "," + newTag;
            }

            currentPhoto.tag = updatedTags;
            photoDao.update(currentPhoto); // Use the update method instead of custom query

            runOnUiThread(() -> {
                displayTags(updatedTags);
                Toast.makeText(this, "Tag added: " + newTag, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void removeTag(String tagToRemove) {
        executorService.execute(() -> {
            String currentTags = currentPhoto.tag;
            if (currentTags != null && !currentTags.isEmpty()) {
                String[] tags = currentTags.split(",");
                StringBuilder updatedTags = new StringBuilder();

                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.equals(tagToRemove) && !trimmedTag.isEmpty()) {
                        if (updatedTags.length() > 0) {
                            updatedTags.append(",");
                        }
                        updatedTags.append(trimmedTag);
                    }
                }

                currentPhoto.tag = updatedTags.toString();
                photoDao.update(currentPhoto);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Tag removed: " + tagToRemove, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}