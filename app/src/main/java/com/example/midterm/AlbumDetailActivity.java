package com.example.midterm;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {
    RecyclerView recyclerViewPhotos;
    TextView textAlbumTitle;
    PhotoDao photoDao;
    ImageAdapter imageAdapter;
    List<Photo> photos;
    int albumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        textAlbumTitle = findViewById(R.id.textAlbumTitle);

        AppDatabase db = AppDatabase.getDatabase(this);
        photoDao = db.photoDao();

        albumId = getIntent().getIntExtra("albumId", -1);
        String albumName = getIntent().getStringExtra("albumName");
        textAlbumTitle.setText(albumName);

        recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        loadPhotos();
    }

    private void loadPhotos() {
        photos = photoDao.getPhotosInAlbum(albumId);

        // Convert List<Photo> → List<Uri>
        List<Uri> photoUris = new ArrayList<>();
        for (Photo p : photos) {
            photoUris.add(Uri.parse(p.uri));
        }

        imageAdapter = new ImageAdapter(this, photoUris);
        recyclerViewPhotos.setAdapter(imageAdapter);
    }
}
