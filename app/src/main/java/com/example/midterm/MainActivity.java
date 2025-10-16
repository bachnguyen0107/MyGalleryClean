package com.example.midterm;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_IMAGES = 100;
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private static final String TAG = "MainActivity";
//    private boolean isSearching = false;
//    private String currentSearchQuery = "";
//    private LinearLayout searchHeader;
//    private TextView tvSearchResults;
//    private Button btnCancelSearch;

    RecyclerView recyclerView;
    ImageAdapter imageAdapter;
    List<Uri> imageUris = new ArrayList<>();

    Spinner albumSpinner;
    FloatingActionButton fabAddAlbum;
    MaterialButton btnAddPhoto;
    ImageButton btnRenameAlbum;

    SharedPreferences prefs;
    Map<String, List<String>> albums = new HashMap<>();
    String currentAlbum = "All Photos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI references
        recyclerView = findViewById(R.id.recyclerViewPhotos);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        albumSpinner = findViewById(R.id.albumSpinner);
        fabAddAlbum = findViewById(R.id.fabAddAlbum);
        btnRenameAlbum = findViewById(R.id.btnRenameAlbum);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);

        prefs = getSharedPreferences("PhotoAlbums", MODE_PRIVATE);
        loadAlbums();

        setupAlbumSpinner();
        checkAndLoadImages();

        fabAddAlbum.setOnClickListener(v -> showAddAlbumDialog());
        btnRenameAlbum.setOnClickListener(v -> showRenameAlbumDialog());
        btnAddPhoto.setOnClickListener(v -> openImagePicker());
        //search
        try {
            ImageButton btnSearch = findViewById(R.id.btnSearch);
            btnSearch.setOnClickListener(v -> showSearchDialog());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing search button", e);
        }

    }

    // Album Management

    private void loadAlbums() {
        albums.clear();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            String urisString = prefs.getString(key, "");
            List<String> uris = new ArrayList<>();
            if (!urisString.isEmpty()) {
                for (String u : urisString.split(",")) {
                    uris.add(u);
                }
            }
            albums.put(key, uris);
        }

        if (!albums.containsKey("All Photos")) {
            albums.put("All Photos", new ArrayList<>());
        }
    }

    private void saveAlbums() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (Map.Entry<String, List<String>> entry : albums.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (String u : entry.getValue()) {
                sb.append(u).append(",");
            }
            editor.putString(entry.getKey(), sb.toString());
        }
        editor.apply();
    }

    private void setupAlbumSpinner() {
        List<String> albumNames = new ArrayList<>(albums.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, albumNames);
        albumSpinner.setAdapter(adapter);

        albumSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                currentAlbum = albumNames.get(position);
                if (currentAlbum.equals("All Photos")) {
                    loadImagesFromGallery();
                } else {
                    showAlbumPhotos();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void showAddAlbumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Album");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty() && !albums.containsKey(name)) {
                albums.put(name, new ArrayList<>());
                saveAlbums();
                setupAlbumSpinner();
                Toast.makeText(this, "Album created: " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid or duplicate name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showRenameAlbumDialog() {
        if (currentAlbum.equals("All Photos")) {
            Toast.makeText(this, "Cannot rename 'All Photos'", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Album");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentAlbum);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !albums.containsKey(newName)) {
                List<String> images = albums.remove(currentAlbum);
                albums.put(newName, images);
                saveAlbums();
                setupAlbumSpinner();
                currentAlbum = newName;
                Toast.makeText(this, "Album renamed to: " + newName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid or duplicate name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Add Photo to Album

    private void openImagePicker() {
        if (currentAlbum.equals("All Photos")) {
            Toast.makeText(this, "Select an album to add photos", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                List<String> albumImages = albums.get(currentAlbum);
                if (!albumImages.contains(selectedImage.toString())) {
                    albumImages.add(selectedImage.toString());
                    saveAlbums();
                    showAlbumPhotos();
                    Toast.makeText(this, "Photo added to " + currentAlbum, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Image Loading

    private void checkAndLoadImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_READ_IMAGES);
            } else {
                loadImagesFromGallery();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_READ_IMAGES);
            } else {
                loadImagesFromGallery();
            }
        } else {
            loadImagesFromGallery();
        }
    }

    private void loadImagesFromGallery() {
        imageUris.clear();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};

        try (Cursor cursor = getContentResolver().query(
                uri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = ContentUris.withAppendedId(uri, id);
                    imageUris.add(contentUri);
                }
            }
        }

        imageAdapter = new ImageAdapter(this, imageUris);
        recyclerView.setAdapter(imageAdapter);
        if (imageAdapter != null) {
            imageAdapter.updateData(imageUris);
        }
    }

    private void showAlbumPhotos() {
        List<String> uris = albums.get(currentAlbum);
        List<Uri> albumUris = new ArrayList<>();
        for (String s : uris) {
            albumUris.add(Uri.parse(s));
        }
        imageAdapter = new ImageAdapter(this, albumUris);
        recyclerView.setAdapter(imageAdapter);
    }

    //  Permission Result

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_READ_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImagesFromGallery();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load images.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // Add the search dialog method
    private void showSearchDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Search Photos by Tag");
            builder.setMessage("Enter a tag to search for photos");

            final EditText input = new EditText(this);
            input.setHint("e.g., vacation, family, beach");
            builder.setView(input);

            builder.setPositiveButton("Search", (dialog, which) -> {
                String searchText = input.getText().toString().trim();
                if (!searchText.isEmpty()) {
                    performTagSearch(searchText);
                } else {
                    showToast("Please enter a search term");
                }
            });

            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing search dialog", e);
            showToast("Error opening search");
        }
    }

    private void performTagSearch(String keyword) {
        showToast("Searching for: " + keyword);

        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                PhotoDao photoDao = db.photoDao();

                // First, let's check if we have any photos in the database
                List<Photo> allPhotos = photoDao.getAllPhotos();
                Log.d(TAG, "Total photos in DB: " + allPhotos.size());

                // Now search by tag
                List<Photo> photos = photoDao.searchByTag(keyword);
                Log.d(TAG, "Found " + photos.size() + " photos with tag: " + keyword);

                List<Uri> searchResults = new ArrayList<>();
                for (Photo photo : photos) {
                    try {
                        Uri uri = Uri.parse(photo.uri);
                        searchResults.add(uri);
                    } catch (Exception e) {
                        Log.e(TAG, "Invalid URI: " + photo.uri, e);
                    }
                }

                runOnUiThread(() -> {
                    if (searchResults.isEmpty()) {
                        showToast("No photos found with tag: " + keyword);
                        // Show a sample message and keep current photos
                        imageAdapter.updateData(new ArrayList<>());
                    } else {
                        imageAdapter.updateData(searchResults);
                        showToast("Found " + searchResults.size() + " photos");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                runOnUiThread(() -> {
                    showToast("Search failed: " + e.getMessage());
                    // Fallback: show all images from gallery
                    loadImagesFromGallery();
                });
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
