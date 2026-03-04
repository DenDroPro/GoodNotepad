package com.goodnotepad.ui.screens.allnotes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.goodnotepad.ui.components.AppDrawerContent
import com.goodnotepad.ui.components.NoteCard
import com.goodnotepad.ui.components.NoteListItem
import com.goodnotepad.ui.screens.home.AppBackground
import com.goodnotepad.ui.screens.home.AppFab
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle
import kotlinx.coroutines.launch

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
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToAllNotes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    val notesFlow = remember(listType, folderId) {
        when (listType) {
            NoteListType.ALL -> viewModel.allNotes
            NoteListType.FAVORITES -> viewModel.favoriteNotes
            NoteListType.TRASH -> viewModel.deletedNotes
            NoteListType.FOLDER -> viewModel.getNotesByFolder(folderId)
        }
    }
    val notes by notesFlow.collectAsState()

    val sortedNotes = remember(notes, sortMode) { viewModel.sortNotes(notes, sortMode) }

    var showViewModeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showContextMenu by remember { mutableStateOf<Note?>(null) }
    var showHeaderColorDialog by remember { mutableStateOf<Note?>(null) }
    var showMoveFolderDialog by remember { mutableStateOf<Note?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filteredNotes = remember(sortedNotes, searchQuery) {
        if (searchQuery.isBlank()) sortedNotes
        else sortedNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    val screenTitle = when (listType) {
        NoteListType.ALL -> "\u0412\u0441\u0435 \u0437\u0430\u043c\u0435\u0442\u043a\u0438"
        NoteListType.FAVORITES -> "\u0418\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435"
        NoteListType.TRASH -> "\u041a\u043e\u0440\u0437\u0438\u043d\u0430"
        NoteListType.FOLDER -> title
    }

    val selectedDrawerItem = when (listType) {
        NoteListType.FOLDER -> "folders"
        NoteListType.ALL -> "all_notes"
        NoteListType.FAVORITES -> "favorites"
        NoteListType.TRASH -> "trash"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                selectedItem = selectedDrawerItem,
                onNavigateToFolders = { scope.launch { drawerState.close() }; onNavigateToFolders() },
                onNavigateToAllNotes = { scope.launch { drawerState.close() }; onNavigateToAllNotes() },
                onNavigateToFavorites = { scope.launch { drawerState.close() }; onNavigateToFavorites() },
                onNavigateToTrash = { scope.launch { drawerState.close() }; onNavigateToTrash() },
                onNavigateToSettings = { scope.launch { drawerState.close() }; onNavigateToSettings() }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (showSearch) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("\u041f\u043e\u0438\u0441\u043a...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(screenTitle, fontWeight = FontWeight.Bold, color = AppTitle)
                        }
                    },
                    navigationIcon = {
                        if (showSearch) {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "\u0417\u0430\u043a\u0440\u044b\u0442\u044c")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "\u041c\u0435\u043d\u044e", tint = AppTitle)
                            }
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            Box {
                                IconButton(onClick = { showViewModeMenu = true }) {
                                    Icon(if (viewMode == ViewMode.LIST) Icons.Default.ViewList else Icons.Default.GridView, contentDescription = "\u0412\u0438\u0434", tint = AppTitle)
                                }
                                DropdownMenu(expanded = showViewModeMenu, onDismissRequest = { showViewModeMenu = false }) {
                                    DropdownMenuItem(text = { Text("\u0421\u043f\u0438\u0441\u043e\u043a") }, onClick = { viewModel.setViewMode(ViewMode.LIST); showViewModeMenu = false }, leadingIcon = { Icon(Icons.Default.ViewList, null) })
                                    DropdownMenuItem(text = { Text("\u0421\u0435\u0442\u043a\u0430 2x2") }, onClick = { viewModel.setViewMode(ViewMode.GRID_2); showViewModeMenu = false }, leadingIcon = { Icon(Icons.Default.GridView, null) })
                                    DropdownMenuItem(text = { Text("\u0421\u0435\u0442\u043a\u0430 3x3") }, onClick = { viewModel.setViewMode(ViewMode.GRID_3); showViewModeMenu = false }, leadingIcon = { Icon(Icons.Default.GridView, null) })
                                    DropdownMenuItem(text = { Text("\u0421\u0435\u0442\u043a\u0430 4x4") }, onClick = { viewModel.setViewMode(ViewMode.GRID_4); showViewModeMenu = false }, leadingIcon = { Icon(Icons.Default.GridView, null) })
                                    DropdownMenuItem(text = { Text("\u0421\u0435\u0442\u043a\u0430 5x5") }, onClick = { viewModel.setViewMode(ViewMode.GRID_5); showViewModeMenu = false }, leadingIcon = { Icon(Icons.Default.GridView, null) })
                                }
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "\u0421\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430", tint = AppTitle)
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    SortMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.label, fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = { viewModel.setSortMode(mode); showSortMenu = false }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "\u041f\u043e\u0438\u0441\u043a", tint = AppTitle)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppHeader)
                )
            },
            floatingActionButton = {
                if (listType == NoteListType.FOLDER) {
                    SmallFloatingActionButton(
                        onClick = { showCreateNoteDialog = true },
                        containerColor = AppFab,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "\u041d\u043e\u0432\u0430\u044f \u0437\u0430\u043c\u0435\u0442\u043a\u0430")
                    }
                }
            },
            containerColor = AppBackground
        ) { padding ->
            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(64.dp), tint = Color(0xFFBBBBBB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("\u041d\u0435\u0442 \u0437\u0430\u043c\u0435\u0442\u043e\u043a", fontSize = 18.sp, color = Color(0xFF999999))
                        if (listType == NoteListType.FOLDER) {
                            Text("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 + \u0447\u0442\u043e\u0431\u044b \u0441\u043e\u0437\u0434\u0430\u0442\u044c", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                        }
                    }
                }
            } else {
                when (viewMode) {
                    ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteListItem(note = note, onClick = { onNavigateToEditor(note.id) }, onLongClick = { showContextMenu = note })
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
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCard(note = note, onClick = { onNavigateToEditor(note.id) }, onLongClick = { showContextMenu = note }, viewMode = viewMode)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateNoteDialog) {
        CreateNoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onCreate = { name, headerColor ->
                viewModel.createNoteWithDetails(folderId = folderId, title = name, headerColor = headerColor) { noteId -> onNavigateToEditor(noteId) }
                showCreateNoteDialog = false
            }
        )
    }

    showContextMenu?.let { note ->
        if (listType == NoteListType.TRASH) {
            TrashContextMenuDialog(note = note, onDismiss = { showContextMenu = null },
                onRestore = { viewModel.restoreNote(note.id); showContextMenu = null },
                onDeleteForever = { viewModel.permanentlyDeleteNote(note); showContextMenu = null })
        } else {
            NoteContextMenuDialog(note = note, onDismiss = { showContextMenu = null },
                onOpen = { showContextMenu = null; onNavigateToEditor(note.id) },
                onToggleFavorite = { viewModel.toggleNoteFavorite(note.id); showContextMenu = null },
                onTogglePin = { viewModel.toggleNotePin(note.id); showContextMenu = null },
                onChangeHeaderColor = { showContextMenu = null; showHeaderColorDialog = note },
                onMoveToFolder = { showContextMenu = null; showMoveFolderDialog = note },
                onDelete = { viewModel.softDeleteNote(note.id); showContextMenu = null })
        }
    }

    showHeaderColorDialog?.let { note ->
        HeaderColorDialog(currentColor = note.headerColor, onDismiss = { showHeaderColorDialog = null },
            onColorSelected = { color -> viewModel.changeNoteHeaderColor(note.id, color); showHeaderColorDialog = null })
    }

    showMoveFolderDialog?.let { note ->
        val folders by viewModel.folders.collectAsState()
        MoveFolderDialog(folders = folders, currentFolderId = note.folderId, onDismiss = { showMoveFolderDialog = null },
            onMove = { targetFolderId -> viewModel.moveNoteToFolder(note.id, targetFolderId); showMoveFolderDialog = null })
    }
}

@Composable
fun CreateNoteDialog(onDismiss: () -> Unit, onCreate: (String, HeaderColor) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(HeaderColor.NONE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u041d\u043e\u0432\u044b\u0439 \u0444\u0430\u0439\u043b") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("\u0426\u0432\u0435\u0442 \u0448\u0430\u043f\u043a\u0438:", fontSize = 14.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFCCCCCC)), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { selectedColor = HeaderColor.NONE }) {
                            if (selectedColor == HeaderColor.NONE) Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("\u0411\u0435\u0437 \u0446\u0432\u0435\u0442\u0430", fontSize = 12.sp, color = Color(0xFF888888))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("\u041f\u0430\u0441\u0442\u0435\u043b\u044c\u043d\u044b\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("PASTEL") }.forEach { color ->
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { selectedColor = color }) {
                                if (color == selectedColor) Icon(Icons.Default.Check, null, tint = Color(0xFF555555), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("\u042f\u0440\u043a\u0438\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("VIBRANT") }.forEach { color ->
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { selectedColor = color }) {
                                if (color == selectedColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name, selectedColor) }, enabled = name.isNotBlank()) { Text("\u0421\u043e\u0437\u0434\u0430\u0442\u044c") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}

@Composable
fun NoteContextMenuDialog(note: Note, onDismiss: () -> Unit, onOpen: () -> Unit, onToggleFavorite: () -> Unit, onTogglePin: () -> Unit, onChangeHeaderColor: () -> Unit, onMoveToFolder: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { "\u0411\u0435\u0437 \u0437\u0430\u0433\u043e\u043b\u043e\u0432\u043a\u0430" }) },
        text = {
            Column {
                TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u041e\u0442\u043a\u0440\u044b\u0442\u044c")
                    }
                }
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                        Text(if (note.isFavorite) "\u0423\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0433\u043e" else "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435")
                    }
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                        Text(if (note.isPinned) "\u041e\u0442\u043a\u0440\u0435\u043f\u0438\u0442\u044c" else "\u0417\u0430\u043a\u0440\u0435\u043f\u0438\u0442\u044c")
                    }
                }
                TextButton(onClick = onChangeHeaderColor, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u0426\u0432\u0435\u0442 \u0448\u0430\u043f\u043a\u0438")
                    }
                }
                TextButton(onClick = onMoveToFolder, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DriveFileMove, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u0432 \u043f\u0430\u043f\u043a\u0443")
                    }
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c") } }
    )
}

@Composable
fun TrashContextMenuDialog(note: Note, onDismiss: () -> Unit, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { "\u0411\u0435\u0437 \u0437\u0430\u0433\u043e\u043b\u043e\u0432\u043a\u0430" }) },
        text = {
            Column {
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestoreFromTrash, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c")
                    }
                }
                TextButton(onClick = onDeleteForever, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, null, tint = Color.Red, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043d\u0430\u0432\u0441\u0435\u0433\u0434\u0430", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c") } }
    )
}

@Composable
fun HeaderColorDialog(currentColor: HeaderColor, onDismiss: () -> Unit, onColorSelected: (HeaderColor) -> Unit) {
    var selected by remember { mutableStateOf(currentColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u0426\u0432\u0435\u0442 \u0448\u0430\u043f\u043a\u0438") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFCCCCCC)), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { selected = HeaderColor.NONE }) {
                            if (selected == HeaderColor.NONE) Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("\u0411\u0435\u0437 \u0446\u0432\u0435\u0442\u0430", fontSize = 14.sp, color = Color(0xFF666666))
                }
                Text("\u041f\u0430\u0441\u0442\u0435\u043b\u044c\u043d\u044b\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("PASTEL") }.forEach { color ->
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { selected = color }) {
                                if (color == selected) Icon(Icons.Default.Check, null, tint = Color(0xFF555555), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Text("\u042f\u0440\u043a\u0438\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("VIBRANT") }.forEach { color ->
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { selected = color }) {
                                if (color == selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(selected) }) { Text("\u041f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}

@Composable
fun MoveFolderDialog(folders: List<Folder>, currentFolderId: Long, onDismiss: () -> Unit, onMove: (Long) -> Unit) {
    var selectedFolderId by remember { mutableStateOf(currentFolderId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u0432 \u043f\u0430\u043f\u043a\u0443") },
        text = {
            Column {
                folders.forEach { folder ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedFolderId == folder.id, onClick = { selectedFolderId = folder.id })
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onMove(selectedFolderId) }) { Text("\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}
