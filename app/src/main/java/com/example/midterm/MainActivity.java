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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private boolean isSearching = false;
    private String currentSearchQuery = "";
    private LinearLayout searchHeader;
    private TextView tvSearchResults;
    private Button btnCancelSearch;
    private ImageButton btnToggleView;
    private boolean isGridView = true;
    private GridLayoutManager gridLayoutManager;
    private LinearLayoutManager listLayoutManager;
    private MaterialButton btnSlideshow;


    RecyclerView recyclerView;
    ImageAdapter imageAdapter;
    List<Uri> imageUris = new ArrayList<>();

    Spinner albumSpinner;
    FloatingActionButton fabAddAlbum;
    MaterialButton btnAddPhoto;
    ImageButton btnRenameAlbum;
    ImageButton btnSearch;

    SharedPreferences prefs;
    Map<String, List<String>> albums = new HashMap<>();
    String currentAlbum = "All Photos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize ALL UI elements
        initializeUI();

        prefs = getSharedPreferences("PhotoAlbums", MODE_PRIVATE);
        loadAlbums();
        setupAlbumSpinner();
        checkAndLoadImages();

        setupClickListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSearching) {
                    cancelSearch();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });
    }

    private void initializeUI() {
        // Initialize search UI elements
        searchHeader = findViewById(R.id.searchHeader);
        tvSearchResults = findViewById(R.id.tvSearchResults);
        btnCancelSearch = findViewById(R.id.btnCancelSearch);

        // UI references
        recyclerView = findViewById(R.id.recyclerViewPhotos);
        albumSpinner = findViewById(R.id.albumSpinner);
        fabAddAlbum = findViewById(R.id.fabAddAlbum);
        btnRenameAlbum = findViewById(R.id.btnRenameAlbum);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnSearch = findViewById(R.id.btnSearch);
        btnToggleView = findViewById(R.id.btnToggleView);

        // Setup layout managers
        gridLayoutManager = new GridLayoutManager(this, 3); // 3 columns for grid
        listLayoutManager = new LinearLayoutManager(this); // 1 column for list

        // Start with grid view
        recyclerView.setLayoutManager(gridLayoutManager);

        // Initialize adapter with empty list
        imageAdapter = new ImageAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(imageAdapter);
        //Slideshow button
        btnSlideshow = findViewById(R.id.btnSlideshow);
    }

    private void setupClickListeners() {
        // Set up cancel search button
        btnCancelSearch.setOnClickListener(v -> cancelSearch());

        fabAddAlbum.setOnClickListener(v -> showAddAlbumDialog());
        btnRenameAlbum.setOnClickListener(v -> showRenameAlbumDialog());
        btnAddPhoto.setOnClickListener(v -> openImagePicker());
        btnSearch.setOnClickListener(v -> showSearchDialog());
        btnToggleView.setOnClickListener(v -> toggleView());
        btnSlideshow.setOnClickListener(v -> showSlideshowDialog());
    }

    // Album Management
    private void loadAlbums() {
        albums.clear();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            String urisString = prefs.getString(key, "");
            List<String> uris = new ArrayList<>();
            if (!urisString.isEmpty()) {
                String[] uriArray = urisString.split(",");
                for (String u : uriArray) {
                    if (!u.isEmpty()) {
                        uris.add(u);
                    }
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
                if (!u.isEmpty()) {
                    sb.append(u).append(",");
                }
            }
            // Remove trailing comma
            String result = sb.toString();
            if (result.endsWith(",")) {
                result = result.substring(0, result.length() - 1);
            }
            editor.putString(entry.getKey(), result);
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
                String selectedAlbum = albumNames.get(position);
                if (!selectedAlbum.equals(currentAlbum)) {
                    currentAlbum = selectedAlbum;
                    if (isSearching) {
                        cancelSearch();
                    }
                    if (currentAlbum.equals("All Photos")) {
                        loadImagesFromGallery();
                    } else {
                        showAlbumPhotos();
                    }
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
        input.setHint("Enter album name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty() && !albums.containsKey(name)) {
                albums.put(name, new ArrayList<>());
                saveAlbums();
                setupAlbumSpinner();

                // Select the new album
                for (int i = 0; i < albumSpinner.getCount(); i++) {
                    if (albumSpinner.getItemAtPosition(i).equals(name)) {
                        albumSpinner.setSelection(i);
                        break;
                    }
                }

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

                // Select the renamed album
                for (int i = 0; i < albumSpinner.getCount(); i++) {
                    if (albumSpinner.getItemAtPosition(i).equals(newName)) {
                        albumSpinner.setSelection(i);
                        break;
                    }
                }

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
                String imageUriString = selectedImage.toString();

                if (!albumImages.contains(imageUriString)) {
                    albumImages.add(imageUriString);
                    saveAlbums();
                    showAlbumPhotos();
                    Toast.makeText(this, "Photo added to " + currentAlbum, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Photo already in album", Toast.LENGTH_SHORT).show();
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
        new Thread(() -> {
            List<Uri> loadedUris = new ArrayList<>();
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
                        loadedUris.add(contentUri);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading images from gallery", e);
            }

            List<Uri> finalLoadedUris = loadedUris;
            runOnUiThread(() -> {
                imageUris = finalLoadedUris;
                imageAdapter.updateData(imageUris);
                applyCurrentViewType();
                Log.d(TAG, "Loaded " + imageUris.size() + " images from gallery");
            });
        }).start();
    }

    private void showAlbumPhotos() {
        List<String> uriStrings = albums.get(currentAlbum);
        List<Uri> albumUris = new ArrayList<>();

        if (uriStrings != null) {
            for (String uriString : uriStrings) {
                try {
                    if (!uriString.isEmpty()) {
                        Uri uri = Uri.parse(uriString);
                        albumUris.add(uri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Invalid URI in album: " + uriString, e);
                }
            }
        }

        imageAdapter.updateData(albumUris);
        applyCurrentViewType(); // ADD THIS LINE
        Log.d(TAG, "Showing " + albumUris.size() + " photos in album: " + currentAlbum);
    }

    // View Toggle Methods
    private void toggleView() {
        isGridView = !isGridView;

        if (isGridView) {
            // Switch to grid view
            recyclerView.setLayoutManager(gridLayoutManager);
            btnToggleView.setImageResource(R.drawable.ic_list_view);
            btnToggleView.setContentDescription("Switch to list view");
            if (imageAdapter != null) {
                imageAdapter.setViewType(ImageAdapter.VIEW_TYPE_GRID);
            }
            showToast("Grid View");
        } else {
            // Switch to list view
            recyclerView.setLayoutManager(listLayoutManager);
            btnToggleView.setImageResource(R.drawable.ic_grid_view);
            btnToggleView.setContentDescription("Switch to grid view");
            if (imageAdapter != null) {
                imageAdapter.setViewType(ImageAdapter.VIEW_TYPE_LIST);
            }
            showToast("List View");
        }
    }

    private void applyCurrentViewType() {
        if (isGridView) {
            recyclerView.setLayoutManager(gridLayoutManager);
            if (imageAdapter != null) {
                imageAdapter.setViewType(ImageAdapter.VIEW_TYPE_GRID);
            }
        } else {
            recyclerView.setLayoutManager(listLayoutManager);
            if (imageAdapter != null) {
                imageAdapter.setViewType(ImageAdapter.VIEW_TYPE_LIST);
            }
        }
    }

    // Search Functionality
    private void showSearchResultsHeader(String query, int resultCount) {
        isSearching = true;
        currentSearchQuery = query;

        String resultsText;
        if (resultCount == 0) {
            resultsText = "No results for: \"" + query + "\"";
        } else {
            resultsText = resultCount + " results for: \"" + query + "\"";
        }

        tvSearchResults.setText(resultsText);
        searchHeader.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
        params.removeRule(RelativeLayout.BELOW); // Clear existing rules
        params.addRule(RelativeLayout.BELOW, R.id.searchHeader);
        recyclerView.setLayoutParams(params);

        recyclerView.requestLayout();
    }

    private void cancelSearch() {
        isSearching = false;
        currentSearchQuery = "";
        searchHeader.setVisibility(View.GONE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
        params.removeRule(RelativeLayout.BELOW); // Clear existing rules
        params.addRule(RelativeLayout.BELOW, R.id.albumHeader);
        recyclerView.setLayoutParams(params);

        recyclerView.requestLayout();

        // Reload the original content based on current album
        if (currentAlbum.equals("All Photos")) {
            loadImagesFromGallery();
        } else {
            showAlbumPhotos();
        }

        applyCurrentViewType();
        showToast("Search cancelled");
        Log.d(TAG, "Search cancelled, returning to: " + currentAlbum);
    }

    private void showSearchDialog() {
        // If already searching, offer to cancel or new search
        if (isSearching) {
            new AlertDialog.Builder(this)
                    .setTitle("Search Active")
                    .setMessage("You're currently searching for: \"" + currentSearchQuery + "\"")
                    .setPositiveButton("New Search", (dialog, which) -> showSearchInputDialog())
                    .setNegativeButton("Cancel Search", (dialog, which) -> cancelSearch())
                    .setNeutralButton("Close", null)
                    .show();
        } else {
            showSearchInputDialog();
        }
    }

    private void showSearchInputDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Search Photos by Tag");

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
            builder.show();
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
                List<Photo> photos = photoDao.searchByTag(keyword);

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
                        showSearchResultsHeader(keyword, 0);
                    } else {
                        showSearchResultsHeader(keyword, searchResults.size());
                    }
                    imageAdapter.updateData(searchResults);
                    applyCurrentViewType(); // ADD THIS LINE
                });

            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                runOnUiThread(() -> {
                    showToast("Search failed: " + e.getMessage());
                    cancelSearch();
                });
            }
        });
    }
    //Show Slideshow Options
    private void showSlideshowDialog() {
        int currentImageCount = (imageUris != null) ? imageUris.size() : 0;
        Log.d(TAG, "Slideshow dialog - Current images: " + currentImageCount);

        if (currentImageCount == 0) {
            Toast.makeText(this, "No images available for slideshow", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Slideshow");
        builder.setMessage("Select " + currentImageCount + " images from current view or choose specific images:");

        builder.setPositiveButton("Current View (" + currentImageCount + " images)", (dialog, which) -> {
            Log.d(TAG, "User selected current view slideshow");
            startSlideshow(imageUris);
        });

        // For now, we'll use the same functionality for both options
        // You can implement image selection later
        builder.setNegativeButton("Select Images", (dialog, which) -> {
            Log.d(TAG, "User selected custom image selection");
            // For now, use current view since multi-select is complex
            startSlideshow(imageUris);
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

//    private void showImageSelectionDialog() {
//        //  open a multi-select dialog for choosing specific images
//        startSlideshow(imageUris);
//    }

    private void startSlideshow(List<Uri> uris) {
        Log.d(TAG, "Starting slideshow with " + (uris != null ? uris.size() : 0) + " images");

        if (uris == null || uris.isEmpty()) {
            Toast.makeText(this, "No images available for slideshow", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(this, SlideShowActivity.class);
            ArrayList<String> uriStrings = new ArrayList<>();
            for (Uri uri : uris) {
                if (uri != null) {
                    uriStrings.add(uri.toString());
                    Log.d(TAG, "Adding URI to slideshow: " + uri);
                }
            }

            if (uriStrings.isEmpty()) {
                Toast.makeText(this, "No valid images selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Store list in in-memory cache to avoid TransactionTooLargeException
            String cacheKey = SlideshowCache.put(uriStrings);
            intent.putExtra("slideCacheKey", cacheKey);
            // Also keep a tiny fallback of the first few to be safe (optional)
            // intent.putStringArrayListExtra("slideUris", new ArrayList<>(uriStrings.subList(0, Math.min(5, uriStrings.size()))));

            startActivity(intent);
            Log.d(TAG, "Slideshow activity started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting slideshow: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting slideshow: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    // Permission Result
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

