package com.goodnotepad.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.goodnotepad.data.Folder
import com.goodnotepad.data.FolderColor
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.components.AppDrawerContent
import kotlinx.coroutines.launch

// App colors
val AppBackground = Color(0xFFF5F0E8)
val AppHeader = Color(0xFFF5E6C8)
val AppTitle = Color(0xFF5D4E37)
val AppFab = Color(0xFFD2691E)
val AppAccent = Color(0xFF8B6914)
val DrawerBackground = Color(0xFFFFFBF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNavigateToNotes: (Long) -> Unit,
    onNavigateToAllNotes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val folders by viewModel.folders.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showColorDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    var showContextMenu by remember { mutableStateOf<Folder?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Issue #9: Collect note counts for each folder
    val noteCounts = remember { mutableStateMapOf<Long, Int>() }
    LaunchedEffect(folders) {
        folders.forEach { folder ->
            val count = viewModel.getNoteCountForFolder(folder.id)
            noteCounts[folder.id] = count
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                selectedItem = "folders",
                onNavigateToFolders = { scope.launch { drawerState.close() } },
                onNavigateToAllNotes = {
                    scope.launch { drawerState.close() }
                    onNavigateToAllNotes()
                },
                onNavigateToFavorites = {
                    scope.launch { drawerState.close() }
                    onNavigateToFavorites()
                },
                onNavigateToTrash = {
                    scope.launch { drawerState.close() }
                    onNavigateToTrash()
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
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
                                placeholder = { Text("\u041f\u043e\u0438\u0441\u043a \u043f\u0430\u043f\u043e\u043a...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "\u041f\u0430\u043f\u043a\u0438",
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
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "\u041f\u043e\u0438\u0441\u043a", tint = AppTitle)
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", tint = AppTitle)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppHeader)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = AppFab,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u043f\u0430\u043f\u043a\u0443")
                }
            },
            containerColor = AppBackground
        ) { padding ->
            val filteredFolders = if (searchQuery.isBlank()) folders
            else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFBBBBBB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("\u041d\u0435\u0442 \u043f\u0430\u043f\u043e\u043a", fontSize = 18.sp, color = Color(0xFF999999))
                        Text("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 + \u0447\u0442\u043e\u0431\u044b \u0441\u043e\u0437\u0434\u0430\u0442\u044c", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredFolders, key = { it.id }) { folder ->
                        FolderListItem(
                            folder = folder,
                            noteCount = noteCounts[folder.id] ?: 0,
                            onClick = { onNavigateToNotes(folder.id) },
                            onLongClick = { showContextMenu = folder }
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, color ->
                viewModel.createFolder(name, color)
                showCreateFolderDialog = false
            }
        )
    }

    showContextMenu?.let { folder ->
        FolderContextMenuDialog(
            folder = folder,
            onDismiss = { showContextMenu = null },
            onRename = { showContextMenu = null; showRenameFolderDialog = folder },
            onChangeColor = { showContextMenu = null; showColorDialog = folder },
            onTogglePin = { viewModel.toggleFolderPin(folder.id); showContextMenu = null },
            onDelete = { showContextMenu = null; showDeleteDialog = folder }
        )
    }

    showRenameFolderDialog?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            onDismiss = { showRenameFolderDialog = null },
            onRename = { name -> viewModel.renameFolder(folder.id, name); showRenameFolderDialog = null }
        )
    }

    showColorDialog?.let { folder ->
        FolderColorDialog(
            currentColor = folder.color,
            onDismiss = { showColorDialog = null },
            onColorSelected = { color -> viewModel.changeFolderColor(folder.id, color); showColorDialog = null }
        )
    }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u0430\u043f\u043a\u0443?") },
            text = { Text("\u0412\u0441\u0435 \u0437\u0430\u043c\u0435\u0442\u043a\u0438 \u0438\u0437 \u043f\u0430\u043f\u043a\u0438 \u0431\u0443\u0434\u0443\u0442 \u0443\u0434\u0430\u043b\u0435\u043d\u044b") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(folder); showDeleteDialog = null }) {
                    Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") }
            }
        )
    }
}

// Issue #2: Compact folder rows
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: Folder,
    noteCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(folder.color.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = folder.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppTitle)
                // Issue #9: Show note count
                Text(
                    text = "$noteCount ${getNoteCountText(noteCount)}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            if (folder.isPinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "\u0417\u0430\u043a\u0440\u0435\u043f\u043b\u0435\u043d\u0430", tint = AppAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun getNoteCountText(count: Int): String {
    val lastTwo = count % 100
    val lastOne = count % 10
    return when {
        lastTwo in 11..19 -> "\u0444\u0430\u0439\u043b\u043e\u0432"
        lastOne == 1 -> "\u0444\u0430\u0439\u043b"
        lastOne in 2..4 -> "\u0444\u0430\u0439\u043b\u0430"
        else -> "\u0444\u0430\u0439\u043b\u043e\u0432"
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String, FolderColor) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FolderColor.BROWN) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u041d\u043e\u0432\u0430\u044f \u043f\u0430\u043f\u043a\u0430") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0430\u043f\u043a\u0438") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FolderColor.entries.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color.color)
                                .then(
                                    if (color == selectedColor)
                                        Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { selectedColor = color }) {
                                if (color == selectedColor) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("\u0421\u043e\u0437\u0434\u0430\u0442\u044c") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}

@Composable
fun FolderContextMenuDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onChangeColor: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name) },
        text = {
            Column {
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c")
                    }
                }
                TextButton(onClick = onChangeColor, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0446\u0432\u0435\u0442")
                    }
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (folder.isPinned) "\u041e\u0442\u043a\u0440\u0435\u043f\u0438\u0442\u044c" else "\u0417\u0430\u043a\u0440\u0435\u043f\u0438\u0442\u044c")
                    }
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c") } }
    )
}

@Composable
fun RenameFolderDialog(folder: Folder, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0430\u043f\u043a\u0438") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onRename(name) },
                enabled = name.isNotBlank()
            ) { Text("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}

@Composable
fun FolderColorDialog(currentColor: FolderColor, onDismiss: () -> Unit, onColorSelected: (FolderColor) -> Unit) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u0426\u0432\u0435\u0442 \u043f\u0430\u043f\u043a\u0438") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FolderColor.entries.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.color),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { selectedColor = color }) {
                            if (color == selectedColor) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(selectedColor) }) { Text("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") } }
    )
}
