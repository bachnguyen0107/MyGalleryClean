package com.example.midterm;

import android.media.MediaPlayer;
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
    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private boolean isMusicPlaying = false;
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

        // Auto-start the slideshow and music
        startSlideshow();
        startBackgroundMusic();
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

            if (uriStrings != null && !uriStrings.isEmpty()) {
                for (String uriString : uriStrings) {
                    try {
                        if (uriString != null && !uriString.isEmpty()) {
                            Uri uri = Uri.parse(uriString);
                            slideShowUris.add(uri);
                        }
                    } catch (Exception e) {
                        Log.e("SlideshowActivity", "Error parsing URI: " + uriString, e);
                    }
                }
            }

            if (slideShowUris.isEmpty()) {
                Toast.makeText(this, "No valid images for slideshow", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Show first image
            showSlide(currentPosition);

        } catch (Exception e) {
            Log.e("SlideshowActivity", "Error loading slideshow data: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading slideshow", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showSlide(int position) {
        if (isFinishing() || isDestroyed()) return;
        if (position >= 0 && slideShowUris != null && position < slideShowUris.size() && imageView != null) {
            try {
                Glide.with(this)
                        .load(slideShowUris.get(position))
                        .into(imageView);

                if (tvCounter != null) {
                    tvCounter.setText((position + 1) + " / " + slideShowUris.size());
                }
            } catch (Throwable t) {
                Log.e("SlideshowActivity", "Error showing slide at position " + position, t);
            }
        }
    }

    // Music Control Methods
    private void startBackgroundMusic() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            // Create media player for background music
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music);

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true); // Loop the music
                mediaPlayer.setVolume(0.7f, 0.7f); // Set volume (0.0 to 1.0)
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d("SlideshowActivity", "Background music started");
            } else {
                Log.e("SlideshowActivity", "Failed to create media player - music file may be missing");
                Toast.makeText(this, "Background music not available", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("SlideshowActivity", "Error starting background music: " + e.getMessage(), e);
            Toast.makeText(this, "Error playing background music", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopBackgroundMusic() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                isMusicPlaying = false;
                Log.d("SlideshowActivity", "Background music stopped");
            }
        } catch (Exception e) {
            Log.e("SlideshowActivity", "Error stopping background music: " + e.getMessage(), e);
        }
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (isMusicPlaying) {
                mediaPlayer.pause();
                isMusicPlaying = false;
                Toast.makeText(this, "Music paused", Toast.LENGTH_SHORT).show();
            } else {
                mediaPlayer.start();
                isMusicPlaying = true;
                Toast.makeText(this, "Music resumed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Button click methods
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

    public void onMusicToggleClick(View view) {
        toggleMusic();
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
        // Pause music when app goes to background
        if (mediaPlayer != null && isMusicPlaying) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume music when app comes to foreground
        if (mediaPlayer != null && isMusicPlaying) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSlideshow();
        stopBackgroundMusic(); // Important: release media player
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
    }
}