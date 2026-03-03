package com.goodnotepad.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Long): Folder?

    @Insert
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("UPDATE folders SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, name: String)

    @Query("UPDATE folders SET isPinned = NOT isPinned WHERE id = :folderId")
    suspend fun togglePin(folderId: Long)

    @Query("UPDATE folders SET viewMode = :viewMode WHERE id = :folderId")
    suspend fun changeViewMode(folderId: Long, viewMode: String)

    @Query("UPDATE folders SET color = :color WHERE id = :folderId")
    suspend fun changeColor(folderId: Long, color: String)
}
