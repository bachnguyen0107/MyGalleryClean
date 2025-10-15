package com.example.midterm;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PhotoDao {
    @Insert
    void insert(Photo photo);

    @Query("SELECT * FROM Photo WHERE album_id = :albumId")
    List<Photo> getPhotosInAlbum(int albumId);

    @Query("SELECT * FROM Photo WHERE tag LIKE '%' || :keyword || '%'")
    List<Photo> searchByTag(String keyword);
}

