package com.example.midterm;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen); // use XML layout

        ImageView imageView = findViewById(R.id.imageViewFullScreen);

        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            Glide.with(this).load(uri).into(imageView);
        }
    }
}
