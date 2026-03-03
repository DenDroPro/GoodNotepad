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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = DrawerBackground
            ) {
                // Drawer header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppHeader)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Хороший блокнот",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTitle
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Папки") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AppHeader.copy(alpha = 0.5f)
                    )
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    label = { Text("Все заметки") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAllNotes()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                    label = { Text("Избранное") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFavorites()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    label = { Text("Корзина") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrash()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
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
                                placeholder = { Text("Поиск папок...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Папки",
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
                                Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню", tint = AppTitle)
                            }
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск", tint = AppTitle)
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = AppTitle)
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
                    Icon(Icons.Default.Add, contentDescription = "Создать папку")
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
                        Text(
                            "Нет папок",
                            fontSize = 18.sp,
                            color = Color(0xFF999999)
                        )
                        Text(
                            "Нажмите + чтобы создать",
                            fontSize = 14.sp,
                            color = Color(0xFFBBBBBB)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFolders, key = { it.id }) { folder ->
                        FolderListItem(
                            folder = folder,
                            onClick = { onNavigateToNotes(folder.id) },
                            onLongClick = { showContextMenu = folder }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, color ->
                viewModel.createFolder(name, color)
                showCreateFolderDialog = false
            }
        )
    }

    // Context Menu Dialog
    showContextMenu?.let { folder ->
        FolderContextMenuDialog(
            folder = folder,
            onDismiss = { showContextMenu = null },
            onRename = {
                showContextMenu = null
                showRenameFolderDialog = folder
            },
            onChangeColor = {
                showContextMenu = null
                showColorDialog = folder
            },
            onTogglePin = {
                viewModel.toggleFolderPin(folder.id)
                showContextMenu = null
            },
            onDelete = {
                showContextMenu = null
                showDeleteDialog = folder
            }
        )
    }

    // Rename Dialog
    showRenameFolderDialog?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            onDismiss = { showRenameFolderDialog = null },
            onRename = { name ->
                viewModel.renameFolder(folder.id, name)
                showRenameFolderDialog = null
            }
        )
    }

    // Color Dialog
    showColorDialog?.let { folder ->
        FolderColorDialog(
            currentColor = folder.color,
            onDismiss = { showColorDialog = null },
            onColorSelected = { color ->
                viewModel.changeFolderColor(folder.id, color)
                showColorDialog = null
            }
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить папку?") },
            text = { Text("Все заметки из папки будут перемещены в 'Без папки'") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder)
                    showDeleteDialog = null
                }) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder color icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(folder.color.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppTitle
                )
                Text(
                    text = "${folder.noteCount} заметок",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                Text(
                    text = formatDate(folder.lastModified),
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            if (folder.isPinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Закреплена",
                    tint = AppAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, FolderColor) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FolderColor.BROWN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая папка") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название папки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
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
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
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
                        Text("Переименовать")
                    }
                }
                TextButton(onClick = onChangeColor, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Изменить цвет")
                    }
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (folder.isPinned) "Открепить" else "Закрепить")
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
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun RenameFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название папки") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onRename(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun FolderColorDialog(
    currentColor: FolderColor,
    onDismiss: () -> Unit,
    onColorSelected: (FolderColor) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет папки") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
