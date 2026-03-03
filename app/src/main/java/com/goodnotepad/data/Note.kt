package com.goodnotepad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long = 0,
    val title: String = "",
    val content: String = "",
    val preview: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val theme: NoteTheme = NoteTheme.WHITE,
    val pageStyle: PageStyle = PageStyle.BLANK,
    val headerColor: HeaderColor = HeaderColor.NONE,
    val fontSize: Int = 14,
    val textAlign: TextAlign = TextAlign.LEFT,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)
