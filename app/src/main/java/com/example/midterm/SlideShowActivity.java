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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SlideShowActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView tvCounter;
    private SeekBar seekBar;
    private View controlsLayout;

    private List<Uri> slideShowUris;
    private int currentPosition = 0;
    private Handler slideshowHandler;
    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private int slideDuration = 3000; // 3 seconds per slide
    private int transitionDuration = 500; // 500ms transition
    private String slideCacheKey; // persist cache key across config changes


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
                    showSlideWithTransition();
                    if (slideshowHandler != null) {
                        slideshowHandler.postDelayed(this, slideDuration);
                    }
                } else if (currentPosition == slideShowUris.size() - 1) {
                    // End of slideshow
                    stopSlideshow();
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(SlideShowActivity.this, "Slideshow finished", Toast.LENGTH_SHORT).show();
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

        // recover cache key if present
        slideCacheKey = savedInstanceState != null
                ? savedInstanceState.getString("slideCacheKey")
                : getIntent().getStringExtra("slideCacheKey");

        initializeViews();
        loadSlideshowData();
        setupControls();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageViewSlideshow);
        tvCounter = findViewById(R.id.tvSlideCounter);
        seekBar = findViewById(R.id.seekBarSpeed);
        controlsLayout = findViewById(R.id.controlsLayout);

        // Ensure handler runs on main thread
        slideshowHandler = new Handler(Looper.getMainLooper());

        // Start with fully visible image view
        if (imageView != null) {
            imageView.setAlpha(1f);
        }
    }

    private void loadSlideshowData() {
        try {
            // Prefer cache key to avoid large binder transaction
            ArrayList<String> uriStrings = null;
            if (slideCacheKey != null) {
                uriStrings = SlideshowCache.get(slideCacheKey);
                Log.d("SlideshowActivity", "Loaded URIs from cache key: " + (uriStrings != null ? uriStrings.size() : 0));
            }
            if (uriStrings == null) {
                // Fallback to legacy extra
                uriStrings = getIntent().getStringArrayListExtra("slideUris");
                Log.d("SlideshowActivity", "Loaded URIs from intent extra: " + (uriStrings != null ? uriStrings.size() : 0));
            }

            slideShowUris = new ArrayList<>();

            Log.d("SlideshowActivity", "Received " + (uriStrings != null ? uriStrings.size() : 0) + " image URIs");

            if (uriStrings != null && !uriStrings.isEmpty()) {
                for (String uriString : uriStrings) {
                    try {
                        if (uriString != null && !uriString.isEmpty()) {
                            Uri uri = Uri.parse(uriString);
                            slideShowUris.add(uri);
                            Log.d("SlideshowActivity", "Successfully parsed URI: " + uriString);
                        }
                    } catch (Exception e) {
                        Log.e("SlideshowActivity", "Error parsing URI: " + uriString, e);
                    }
                }
            }

            if (slideShowUris == null || slideShowUris.isEmpty()) {
                Toast.makeText(this, "No valid images for slideshow", Toast.LENGTH_LONG).show();
                finish();
                return;
            }


            Log.d("SlideshowActivity", "Loaded " + slideShowUris.size() + " URIs successfully");

            // Show first image
            showSlide(currentPosition);

        } catch (Exception e) {
            Log.e("SlideshowActivity", "Error loading slideshow data: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading slideshow: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupControls() {
        // SeekBar for speed control (1-10 seconds)
        if (seekBar != null) {
            seekBar.setMax(9); // 1-10 seconds
            seekBar.setProgress(2); // Default: 3 seconds (position 2 = 3000ms)

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    slideDuration = (progress + 1) * 1000; // 1-10 seconds
                    if (isPlaying) {
                        restartSlideshow();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
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

    private void showSlideWithTransition() {
        if (isFinishing() || isDestroyed()) return;
        if (slideShowUris == null || slideShowUris.isEmpty() || imageView == null) return;
        if (currentPosition < 0 || currentPosition >= slideShowUris.size()) return;

        try {
            // Fade out current image
            imageView.animate()
                    .alpha(0f)
                    .setDuration(transitionDuration)
                    .withEndAction(() -> {
                        if (isFinishing() || isDestroyed() || imageView == null) return;
                        try {
                            // Load new image
                            Glide.with(this)
                                    .load(slideShowUris.get(currentPosition))
                                    .into(imageView);

                            if (tvCounter != null) {
                                tvCounter.setText((currentPosition + 1) + " / " + slideShowUris.size());
                            }

                            // Fade in new image
                            imageView.animate()
                                    .alpha(1f)
                                    .setDuration(transitionDuration)
                                    .start();
                        } catch (Throwable t) {
                            Log.e("SlideshowActivity", "Error during slide transition", t);
                        }
                    })
                    .start();
        } catch (Throwable t) {
            Log.e("SlideshowActivity", "Error starting transition", t);
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
            showSlideWithTransition();
            if (isPlaying) {
                restartSlideshow();
            }
        }
    }

    public void onNextClick(View view) {
        if (slideShowUris == null) return;
        if (currentPosition < slideShowUris.size() - 1) {
            currentPosition++;
            showSlideWithTransition();
            if (isPlaying) {
                restartSlideshow();
            }
        }
    }

    public void onMusicToggleClick(View view) {
        // Music toggle functionality
        Toast.makeText(this, "Music feature coming soon", Toast.LENGTH_SHORT).show();
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
        // Update play/pause button UI would go here
    }

    private void pauseSlideshow() {
        isPlaying = false;
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
        // Update play/pause button UI would go here
    }

    private void stopSlideshow() {
        isPlaying = false;
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
        // Update play/pause button UI would go here
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
        // Auto-pause when app goes to background to avoid crashes/leaks
        pauseSlideshow();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (slideCacheKey != null) {
            outState.putString("slideCacheKey", slideCacheKey);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSlideshow();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (slideshowHandler != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
        }
        if (imageView != null) {
            imageView.animate().cancel();
        }
        // Only remove cache data if the activity is actually finishing, not on config change
        if (!isChangingConfigurations() && slideCacheKey != null) {
            SlideshowCache.remove(slideCacheKey);
        }
    }
}