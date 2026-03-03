package com.goodnotepad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goodnotepad.NotepadApplication
import com.goodnotepad.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = (application as NotepadApplication).database.noteDao()
    private val folderDao = (application as NotepadApplication).database.folderDao()

    // Folders
    val folders: StateFlow<List<Folder>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All notes
    val allNotes: StateFlow<List<Note>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite notes
    val favoriteNotes: StateFlow<List<Note>> = noteDao.getFavoriteNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deleted notes
    val deletedNotes: StateFlow<List<Note>> = noteDao.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current note being edited
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Note>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else noteDao.searchNotes(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sort mode
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // View mode
    private val _viewMode = MutableStateFlow(ViewMode.GRID_2)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun getNotesByFolder(folderId: Long): StateFlow<List<Note>> {
        return noteDao.getNotesByFolder(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun sortNotes(notes: List<Note>, sortMode: SortMode): List<Note> {
        val pinned = notes.filter { it.isPinned }
        val unpinned = notes.filter { !it.isPinned }
        val sortedUnpinned = when (sortMode) {
            SortMode.CREATED_DESC -> unpinned.sortedByDescending { it.createdAt }
            SortMode.CREATED_ASC -> unpinned.sortedBy { it.createdAt }
            SortMode.UPDATED_DESC -> unpinned.sortedByDescending { it.updatedAt }
            SortMode.UPDATED_ASC -> unpinned.sortedBy { it.updatedAt }
            SortMode.TITLE_ASC -> unpinned.sortedBy { it.title.lowercase() }
            SortMode.TITLE_DESC -> unpinned.sortedByDescending { it.title.lowercase() }
        }
        return pinned + sortedUnpinned
    }

    // Note operations
    fun createNote(folderId: Long = 0, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val note = Note(
                folderId = folderId,
                createdAt = now,
                updatedAt = now
            )
            val id = noteDao.insertNote(note)
            onCreated(id)
        }
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _currentNote.value = noteDao.getNoteById(noteId)
        }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(
                updatedAt = System.currentTimeMillis(),
                preview = note.content.take(100)
            )
            noteDao.updateNote(updated)
            _currentNote.value = updated
        }
    }

    fun toggleNoteFavorite(noteId: Long) {
        viewModelScope.launch {
            noteDao.toggleFavorite(noteId)
        }
    }

    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch {
            noteDao.togglePin(noteId)
        }
    }

    fun changeNoteHeaderColor(noteId: Long, color: HeaderColor) {
        viewModelScope.launch {
            noteDao.changeHeaderColor(noteId, color.name)
        }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        viewModelScope.launch {
            noteDao.moveToFolder(noteId, folderId)
        }
    }

    fun softDeleteNote(noteId: Long) {
        viewModelScope.launch {
            noteDao.softDelete(noteId, System.currentTimeMillis())
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            noteDao.restore(noteId)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteDao.emptyTrash()
        }
    }

    // Folder operations
    fun createFolder(name: String, color: FolderColor) {
        viewModelScope.launch {
            val folder = Folder(
                name = name,
                color = color,
                lastModified = System.currentTimeMillis()
            )
            folderDao.insertFolder(folder)
        }
    }

    fun renameFolder(folderId: Long, name: String) {
        viewModelScope.launch {
            folderDao.renameFolder(folderId, name)
        }
    }

    fun changeFolderColor(folderId: Long, color: FolderColor) {
        viewModelScope.launch {
            folderDao.changeColor(folderId, color.name)
        }
    }

    fun toggleFolderPin(folderId: Long) {
        viewModelScope.launch {
            folderDao.togglePin(folderId)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            // Move all notes from this folder to "no folder"
            val notes = noteDao.getNotesByFolder(folder.id).first()
            notes.forEach { note ->
                noteDao.moveToFolder(note.id, 0)
            }
            folderDao.deleteFolder(folder)
        }
    }

    suspend fun getFolderById(folderId: Long): Folder? {
        return folderDao.getFolderById(folderId)
    }

    suspend fun getNoteCountForFolder(folderId: Long): Int {
        return noteDao.getNoteCountForFolder(folderId)
    }
}
