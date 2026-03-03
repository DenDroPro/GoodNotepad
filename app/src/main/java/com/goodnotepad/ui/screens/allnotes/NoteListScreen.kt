package com.goodnotepad.ui.screens.allnotes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodnotepad.data.*
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.components.NoteCard
import com.goodnotepad.ui.components.NoteListItem
import com.goodnotepad.ui.screens.home.AppBackground
import com.goodnotepad.ui.screens.home.AppFab
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle

enum class NoteListType {
    FOLDER, ALL, FAVORITES, TRASH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    listType: NoteListType,
    folderId: Long = 0,
    title: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    val notes by when (listType) {
        NoteListType.ALL -> viewModel.allNotes.collectAsState()
        NoteListType.FAVORITES -> viewModel.favoriteNotes.collectAsState()
        NoteListType.TRASH -> viewModel.deletedNotes.collectAsState()
        NoteListType.FOLDER -> viewModel.getNotesByFolder(folderId).collectAsState()
    }

    val sortedNotes = viewModel.sortNotes(notes, sortMode)

    var showViewModeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showContextMenu by remember { mutableStateOf<Note?>(null) }
    var showHeaderColorDialog by remember { mutableStateOf<Note?>(null) }
    var showMoveFolderDialog by remember { mutableStateOf<Note?>(null) }

    val filteredNotes = if (searchQuery.isBlank()) sortedNotes
    else sortedNotes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.content.contains(searchQuery, ignoreCase = true)
    }

    val screenTitle = when (listType) {
        NoteListType.ALL -> "Все заметки"
        NoteListType.FAVORITES -> "Избранное"
        NoteListType.TRASH -> "Корзина"
        NoteListType.FOLDER -> title
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            color = AppTitle
                        )
                    }
                },
                navigationIcon = {
                    if (showSearch) {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = AppTitle)
                        }
                    }
                },
                actions = {
                    if (!showSearch) {
                        // View mode
                        Box {
                            IconButton(onClick = { showViewModeMenu = true }) {
                                Icon(
                                    if (viewMode == ViewMode.LIST) Icons.Default.ViewList
                                    else Icons.Default.GridView,
                                    contentDescription = "Вид",
                                    tint = AppTitle
                                )
                            }
                            DropdownMenu(
                                expanded = showViewModeMenu,
                                onDismissRequest = { showViewModeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Список") },
                                    onClick = { viewModel.setViewMode(ViewMode.LIST); showViewModeMenu = false },
                                    leadingIcon = { Icon(Icons.Default.ViewList, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Сетка 2x2") },
                                    onClick = { viewModel.setViewMode(ViewMode.GRID_2); showViewModeMenu = false },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Сетка 3x3") },
                                    onClick = { viewModel.setViewMode(ViewMode.GRID_3); showViewModeMenu = false },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Сетка 4x4") },
                                    onClick = { viewModel.setViewMode(ViewMode.GRID_4); showViewModeMenu = false },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Сетка 5x5") },
                                    onClick = { viewModel.setViewMode(ViewMode.GRID_5); showViewModeMenu = false },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) }
                                )
                            }
                        }

                        // Sort
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Сортировка", tint = AppTitle)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                mode.label,
                                                fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { viewModel.setSortMode(mode); showSortMenu = false }
                                    )
                                }
                            }
                        }

                        // Search
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск", tint = AppTitle)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppHeader)
            )
        },
        floatingActionButton = {
            if (listType != NoteListType.TRASH) {
                FloatingActionButton(
                    onClick = {
                        viewModel.createNote(
                            folderId = if (listType == NoteListType.FOLDER) folderId else 0
                        ) { noteId ->
                            onNavigateToEditor(noteId)
                        }
                    },
                    containerColor = AppFab,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFBBBBBB)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Нет заметок", fontSize = 18.sp, color = Color(0xFF999999))
                    if (listType != NoteListType.TRASH) {
                        Text("Нажмите + чтобы создать", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                    }
                }
            }
        } else {
            when (viewMode) {
                ViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClick = { onNavigateToEditor(note.id) },
                                onLongClick = { showContextMenu = note }
                            )
                        }
                    }
                }
                else -> {
                    val columns = when (viewMode) {
                        ViewMode.GRID_2 -> 2
                        ViewMode.GRID_3 -> 3
                        ViewMode.GRID_4 -> 4
                        ViewMode.GRID_5 -> 5
                        else -> 2
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToEditor(note.id) },
                                onLongClick = { showContextMenu = note }
                            )
                        }
                    }
                }
            }
        }
    }

    // Context menu
    showContextMenu?.let { note ->
        if (listType == NoteListType.TRASH) {
            TrashContextMenuDialog(
                note = note,
                onDismiss = { showContextMenu = null },
                onRestore = {
                    viewModel.restoreNote(note.id)
                    showContextMenu = null
                },
                onDeleteForever = {
                    viewModel.permanentlyDeleteNote(note)
                    showContextMenu = null
                }
            )
        } else {
            NoteContextMenuDialog(
                note = note,
                onDismiss = { showContextMenu = null },
                onToggleFavorite = {
                    viewModel.toggleNoteFavorite(note.id)
                    showContextMenu = null
                },
                onTogglePin = {
                    viewModel.toggleNotePin(note.id)
                    showContextMenu = null
                },
                onChangeHeaderColor = {
                    showContextMenu = null
                    showHeaderColorDialog = note
                },
                onMoveToFolder = {
                    showContextMenu = null
                    showMoveFolderDialog = note
                },
                onDelete = {
                    viewModel.softDeleteNote(note.id)
                    showContextMenu = null
                }
            )
        }
    }

    // Header color dialog
    showHeaderColorDialog?.let { note ->
        HeaderColorDialog(
            currentColor = note.headerColor,
            onDismiss = { showHeaderColorDialog = null },
            onColorSelected = { color ->
                viewModel.changeNoteHeaderColor(note.id, color)
                showHeaderColorDialog = null
            }
        )
    }

    // Move to folder dialog
    showMoveFolderDialog?.let { note ->
        val folders by viewModel.folders.collectAsState()
        MoveFolderDialog(
            folders = folders,
            currentFolderId = note.folderId,
            onDismiss = { showMoveFolderDialog = null },
            onMove = { targetFolderId ->
                viewModel.moveNoteToFolder(note.id, targetFolderId)
                showMoveFolderDialog = null
            }
        )
    }
}

@Composable
fun NoteContextMenuDialog(
    note: Note,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onChangeHeaderColor: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { "Без заголовка" }) },
        text = {
            Column {
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null, modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (note.isFavorite) "Убрать из избранного" else "Добавить в избранное")
                    }
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (note.isPinned) "Открепить" else "Закрепить")
                    }
                }
                TextButton(onClick = onChangeHeaderColor, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Цвет шапки")
                    }
                }
                TextButton(onClick = onMoveToFolder, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Переместить в папку")
                    }
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Удалить", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun TrashContextMenuDialog(
    note: Note,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { "Без заголовка" }) },
        text = {
            Column {
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Восстановить")
                    }
                }
                TextButton(onClick = onDeleteForever, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Удалить навсегда", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun HeaderColorDialog(
    currentColor: HeaderColor,
    onDismiss: () -> Unit,
    onColorSelected: (HeaderColor) -> Unit
) {
    var selected by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет шапки") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderColor.entries.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (color == HeaderColor.NONE) Color(0xFFCCCCCC)
                                else color.color
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { selected = color }) {
                            if (color == HeaderColor.NONE) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            } else if (color == selected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selected) }) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun MoveFolderDialog(
    folders: List<Folder>,
    currentFolderId: Long,
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit
) {
    var selectedFolderId by remember { mutableStateOf(currentFolderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переместить в папку") },
        text = {
            Column {
                // "No folder" option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedFolderId == 0L,
                        onClick = { selectedFolderId = 0L }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Без папки")
                }

                folders.forEach { folder ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMove(selectedFolderId) }) { Text("Переместить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
