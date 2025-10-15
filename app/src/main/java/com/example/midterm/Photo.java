package com.example.midterm;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Photo {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "uri")
    public String uri;
    @ColumnInfo(name = "album_id")
    public int albumId;
    @ColumnInfo(name = "tag")
    public String tag;

    public Photo(String uri, int albumId, String tag) {
        this.uri = uri;
        this.albumId = albumId;
        this.tag = tag;
    }
}

