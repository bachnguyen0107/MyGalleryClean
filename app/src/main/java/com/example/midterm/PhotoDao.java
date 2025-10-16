package com.example.midterm;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PhotoDao {
    @Insert
    void insert(Photo photo);

    @Query("SELECT * FROM Photo WHERE album_id = :albumId")
    List<Photo> getPhotosInAlbum(int albumId);

    @Query("SELECT * FROM Photo WHERE tag LIKE '%' || :keyword || '%'")
    List<Photo> searchByTag(String keyword);

    @Query("SELECT * FROM Photo WHERE uri = :uri")
    Photo getPhotoByUri(String uri);

    @Query("UPDATE Photo SET tag = :tags WHERE id = :id")
    void updatePhotoTags(int id, String tags);

    @Update
    void update(Photo photo);

    // Add this method to get all photos
    @Query("SELECT * FROM Photo")
    List<Photo> getAllPhotos();
}