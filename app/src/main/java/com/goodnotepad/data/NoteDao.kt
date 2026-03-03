package com.goodnotepad.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND isDeleted = 0")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :noteId")
    suspend fun togglePin(noteId: Long)

    @Query("UPDATE notes SET isFavorite = NOT isFavorite WHERE id = :noteId")
    suspend fun toggleFavorite(noteId: Long)

    @Query("UPDATE notes SET headerColor = :color WHERE id = :noteId")
    suspend fun changeHeaderColor(noteId: Long, color: String)

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :noteId")
    suspend fun moveToFolder(noteId: Long, folderId: Long)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :timestamp WHERE id = :noteId")
    suspend fun softDelete(noteId: Long, timestamp: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = 0 WHERE id = :noteId")
    suspend fun restore(noteId: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND isDeleted = 0")
    suspend fun getNoteCountForFolder(folderId: Long): Int
}
