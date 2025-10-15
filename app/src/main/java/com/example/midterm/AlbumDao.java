package com.example.midterm;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlbumDao {
    @Insert
    void insert(Album album);

    @Query("SELECT * FROM Album")
    List<Album> getAllAlbums();

    @Query("DELETE FROM Album WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE Album SET name = :newName WHERE id = :albumId")
    void renameAlbum(int albumId, String newName);
}
