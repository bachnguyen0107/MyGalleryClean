package com.example.midterm;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SlideShowActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView tvCounter;

    private List<Uri> slideShowUris;
    private int currentPosition = 0;
    private Handler slideshowHandler;

    private boolean isPlaying = false;
    private int slideDuration = 3000; // 3 seconds per slide

    private final Runnable slideshowRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!isPlaying || isFinishing() || isDestroyed()) {
                    return;
                }
                if (slideShowUris == null || slideShowUris.isEmpty()) {
                    stopSlideshow();
                    return;
                }
                if (currentPosition < slideShowUris.size() - 1) {
                    currentPosition++;
                    showSlide(currentPosition);
                    if (slideshowHandler != null) {
                        slideshowHandler.postDelayed(this, slideDuration);
                    }
                } else if (currentPosition == slideShowUris.size() - 1) {
                    // End of slideshow - restart from beginning
                    currentPosition = 0;
                    showSlide(currentPosition);
                    if (slideshowHandler != null) {
                        slideshowHandler.postDelayed(this, slideDuration);
                    }
                }
            } catch (Throwable t) {
                Log.e("SlideshowActivity", "Error in slideshow runnable", t);
                stopSlideshow();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slideshow);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeViews();
        loadSlideshowData();

        // Auto-start the slideshow
        startSlideshow();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageViewSlideshow);
        tvCounter = findViewById(R.id.tvSlideCounter);
        slideshowHandler = new Handler(Looper.getMainLooper());
    }

    private void loadSlideshowData() {
        try {
            // Get URIs from intent
            ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("slideUris");
            slideShowUris = new ArrayList<>();

            Log.d("SlideshowActivity", "Received " + (uriStrings != null ? uriStrings.size() : 0) + " image URIs");

            if (uriStrings != null && !uriStrings.isEmpty()) {
                for (String uriString : uriStrings) {
                    try {
                        if (uriString != null && !uriString.isEmpty()) {
                            Uri uri = Uri.parse(uriString);
                            slideShowUris.add(uri);
                            Log.d("SlideshowActivity", "Successfully parsed URI: " + uriString);
                            Log.d("SlideshowActivity", "URI scheme: " + uri.getScheme());
                        }
                    } catch (Exception e) {
                        Log.e("SlideshowActivity", "Error parsing URI: " + uriString, e);
                    }
                }
            }

            if (slideShowUris.isEmpty()) {
                Log.e("SlideshowActivity", "No valid images found for slideshow");
                Toast.makeText(this, "No valid images found for slideshow", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d("SlideshowActivity", "Loaded " + slideShowUris.size() + " images for slideshow");

            // Show first image
            showSlide(currentPosition);

        } catch (Exception e) {
            Log.e("SlideshowActivity", "Error loading slideshow data: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading slideshow: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    private void showSlide(int position) {
        if (isFinishing() || isDestroyed()) return;
        if (position >= 0 && slideShowUris != null && position < slideShowUris.size() && imageView != null) {
            try {
                Uri uri = slideShowUris.get(position);
                Log.d("SlideshowActivity", "Loading slide " + position + ": " + uri);

                Glide.with(this)
                        .load(uri)
                        .error(R.drawable.ic_error)
                        .into(imageView);

                if (tvCounter != null) {
                    tvCounter.setText((position + 1) + " / " + slideShowUris.size());
                }
            } catch (SecurityException e) {
                Log.e("SlideshowActivity", "Security exception loading image: " + e.getMessage());
                Toast.makeText(this, "Permission denied to access images", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                Log.e("SlideshowActivity", "Error showing slide at position " + position, t);
            }
        }
    }
    public void onPlayPauseClick(View view) {
        if (isPlaying) {
            pauseSlideshow();
        } else {
            startSlideshow();
        }
    }

    public void onPreviousClick(View view) {
        if (currentPosition > 0) {
            currentPosition--;
            showSlide(currentPosition);
            if (isPlaying) {
                restartSlideshow();
            }
        }
    }

    public void onNextClick(View view) {
        if (slideShowUris == null) return;
        if (currentPosition < slideShowUris.size() - 1) {
            currentPosition++;
            showSlide(currentPosition);
            if (isPlaying) {
                restartSlideshow();
            }
        }
    }

    public void onCloseClick(View view) {
        finish();
    }

    private void startSlideshow() {
        if (slideShowUris == null || slideShowUris.isEmpty()) {
            Toast.makeText(this, "No images to play", Toast.LENGTH_SHORT).show();
            return;
        }
        isPlaying = true;
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            slideshowHandler.postDelayed(slideshowRunnable, slideDuration);
        }
    }

    private void pauseSlideshow() {
        isPlaying = false;
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
    }

    private void stopSlideshow() {
        isPlaying = false;
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
    }

    private void restartSlideshow() {
        if (isPlaying && slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            slideshowHandler.postDelayed(slideshowRunnable, slideDuration);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseSlideshow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSlideshow();
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
    }
}