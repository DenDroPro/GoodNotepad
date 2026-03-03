package com.goodnotepad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val color: FolderColor = FolderColor.BROWN,
    val icon: FolderIcon = FolderIcon.FOLDER,
    val noteCount: Int = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val viewMode: ViewMode = ViewMode.GRID_2
)
