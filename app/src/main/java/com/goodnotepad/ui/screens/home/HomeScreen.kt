package com.goodnotepad.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodnotepad.data.Folder
import com.goodnotepad.data.FolderColor
import com.goodnotepad.data.FolderIcon
import com.goodnotepad.data.Note
import com.goodnotepad.data.HeaderColor
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.components.AppDrawerContent
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

/** Map FolderIcon enum to Material icon */
fun folderIconToVector(icon: FolderIcon): ImageVector = when (icon) {
    FolderIcon.FOLDER -> Icons.Filled.Folder
    FolderIcon.WORK -> Icons.Filled.Work
    FolderIcon.SCHOOL -> Icons.Filled.School
    FolderIcon.FAVORITE -> Icons.Filled.Favorite
    FolderIcon.MUSIC -> Icons.Filled.MusicNote
    FolderIcon.PHOTO -> Icons.Filled.Photo
    FolderIcon.VIDEO -> Icons.Filled.Videocam
    FolderIcon.TRAVEL -> Icons.Filled.Flight
    FolderIcon.FOOD -> Icons.Filled.Restaurant
    FolderIcon.SPORT -> Icons.Filled.FitnessCenter
    FolderIcon.HEALTH -> Icons.Filled.LocalHospital
    FolderIcon.FINANCE -> Icons.Filled.AttachMoney
    FolderIcon.SHOPPING -> Icons.Filled.ShoppingCart
    FolderIcon.PETS -> Icons.Filled.Pets
    FolderIcon.ART -> Icons.Filled.Palette
    FolderIcon.CODE -> Icons.Filled.Code
    FolderIcon.BOOK -> Icons.Filled.MenuBook
    FolderIcon.GAME -> Icons.Filled.SportsEsports
    FolderIcon.HOME -> Icons.Filled.Home
    FolderIcon.CAR -> Icons.Filled.DirectionsCar
    FolderIcon.CROSS -> Icons.Filled.Add
    FolderIcon.FLOWER -> Icons.Filled.LocalFlorist
    FolderIcon.MONEY -> Icons.Filled.CurrencyRuble
    FolderIcon.RECEIPT -> Icons.Filled.Receipt
    FolderIcon.STAR -> Icons.Filled.Star
    FolderIcon.BABY -> Icons.Filled.ChildCare
    FolderIcon.CHURCH -> Icons.Filled.Church
    FolderIcon.NATURE -> Icons.Filled.Park
    FolderIcon.SCIENCE -> Icons.Filled.Science
    FolderIcon.PHONE -> Icons.Filled.Phone
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNavigateToNotes: (Long) -> Unit,
    onNavigateToEditor: (Long) -> Unit = {},
    onNavigateToAllNotes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val folders by viewModel.folders.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var isGridView by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showColorDialog by remember { mutableStateOf<Folder?>(null) }
    var showIconDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    var showContextMenu by remember { mutableStateOf<Folder?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val recentNotes = remember(allNotes) {
        allNotes.filter { !it.isDeleted }.sortedByDescending { it.updatedAt }.take(10)
    }

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
                                value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("Поиск папок...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(text = "Папки", fontWeight = FontWeight.Bold, color = AppTitle)
                        }
                    },
                    navigationIcon = {
                        if (showSearch) {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню", tint = AppTitle)
                            }
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            IconButton(onClick = { isGridView = !isGridView }) {
                                Icon(
                                    if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = if (isGridView) "Список" else "Сетка",
                                    tint = AppTitle
                                )
                            }
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
                    containerColor = AppFab, contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать папку")
                }
            },
            containerColor = AppBackground
        ) { padding ->
            val filteredFolders = if (searchQuery.isBlank()) folders
            else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filteredFolders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFBBBBBB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Нет папок", fontSize = 18.sp, color = Color(0xFF999999))
                        Text("Нажмите + чтобы создать", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                    }
                }
            } else if (!isGridView) {
                // View 1: List of folders
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
            } else {
                // View 2: Minimal folders grid + Recent notes
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    // Folders grid - 4 columns like reference
                    val columns = 4
                    val rows = (filteredFolders.size + columns - 1) / columns
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (col in 0 until columns) {
                                    val idx = row * columns + col
                                    if (idx < filteredFolders.size) {
                                        val folder = filteredFolders[idx]
                                        FolderGridItemMinimal(
                                            folder = folder,
                                            onClick = { onNavigateToNotes(folder.id) },
                                            onLongClick = { showContextMenu = folder },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Recent notes section
                    if (recentNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Недавние заметки",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            recentNotes.forEach { note ->
                                RecentNoteItem(note = note, onClick = { onNavigateToEditor(note.id) })
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, color, icon ->
                viewModel.createFolderWithIcon(name, color, icon)
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
            onChangeIcon = { showContextMenu = null; showIconDialog = folder },
            onTogglePin = { viewModel.toggleFolderPin(folder.id); showContextMenu = null },
            onDelete = { showContextMenu = null; showDeleteDialog = folder }
        )
    }

    showRenameFolderDialog?.let { folder ->
        RenameFolderDialog(folder = folder, onDismiss = { showRenameFolderDialog = null },
            onRename = { name -> viewModel.renameFolder(folder.id, name); showRenameFolderDialog = null })
    }

    showColorDialog?.let { folder ->
        FolderColorDialog(currentColor = folder.color, onDismiss = { showColorDialog = null },
            onColorSelected = { color -> viewModel.changeFolderColor(folder.id, color); showColorDialog = null })
    }

    showIconDialog?.let { folder ->
        FolderIconDialog(currentIcon = folder.icon, onDismiss = { showIconDialog = null },
            onIconSelected = { icon -> viewModel.changeFolderIcon(folder.id, icon); showIconDialog = null })
    }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить папку?") },
            text = { Text("Все заметки из папки будут удалены") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(folder); showDeleteDialog = null }) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Отмена") } }
        )
    }
}

// Compact list item
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(folder: Folder, noteCount: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(folder.color.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(folderIconToVector(folder.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = folder.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppTitle)
                Text(text = "$noteCount ${getNoteCountText(noteCount)}", fontSize = 12.sp, color = Color(0xFF999999))
            }
            if (folder.isPinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "Закреплена", tint = AppAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// Minimal grid item - like reference: colored rounded square with icon, name below
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItemMinimal(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Colored rounded square with pastel background + icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(folder.color.color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                folderIconToVector(folder.icon),
                contentDescription = null,
                tint = Color(0xFF5D4037),
                modifier = Modifier.size(28.dp)
            )
            // Pin indicator
            if (folder.isPinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = AppAccent,
                    modifier = Modifier.size(10.dp).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Name below
        Text(
            text = folder.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AppTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Recent note item
@Composable
fun RecentNoteItem(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.headerColor != HeaderColor.NONE) {
                Box(modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(note.headerColor.color))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Без заголовка" },
                    fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = Color(0xFF333333)
                )
                Text(
                    text = note.preview.ifBlank { "Пустая заметка" },
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF999999)
                )
            }
            Text(
                text = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(note.updatedAt)),
                fontSize = 10.sp, color = Color(0xFFBBBBBB)
            )
        }
    }
}

private fun getNoteCountText(count: Int): String {
    val lastTwo = count % 100
    val lastOne = count % 10
    return when {
        lastTwo in 11..19 -> "файлов"
        lastOne == 1 -> "файл"
        lastOne in 2..4 -> "файла"
        else -> "файлов"
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String, FolderColor, FolderIcon) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FolderColor.BROWN) }
    var selectedIcon by remember { mutableStateOf(FolderIcon.FOLDER) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая папка") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название папки") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Цвет", fontSize = 13.sp, color = Color(0xFF777777))
                Spacer(modifier = Modifier.height(4.dp))
                // Color row - horizontal scroll
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    FolderColor.entries.forEach { color ->
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.color)
                                .then(if (color == selectedColor) Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { selectedColor = color }, modifier = Modifier.size(32.dp)) {
                                if (color == selectedColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Иконка", fontSize = 13.sp, color = Color(0xFF777777))
                Spacer(modifier = Modifier.height(4.dp))
                // Icon grid - 5 per row
                val icons = FolderIcon.entries.toList()
                for (rowIdx in 0..(icons.size - 1) / 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (colIdx in 0 until 5) {
                            val idx = rowIdx * 5 + colIdx
                            if (idx < icons.size) {
                                val icon = icons[idx]
                                val isSelected = icon == selectedIcon
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) selectedColor.color.copy(alpha = 0.3f) else Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { selectedIcon = icon }, modifier = Modifier.size(36.dp)) {
                                        Icon(folderIconToVector(icon), null, tint = if (isSelected) Color(0xFF5D4037) else Color(0xFF888888), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (rowIdx < (icons.size - 1) / 5) Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, selectedColor, selectedIcon) }, enabled = name.isNotBlank()) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun FolderContextMenuDialog(
    folder: Folder, onDismiss: () -> Unit, onRename: () -> Unit,
    onChangeColor: () -> Unit, onChangeIcon: () -> Unit, onTogglePin: () -> Unit, onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name) },
        text = {
            Column {
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Переименовать")
                    }
                }
                TextButton(onClick = onChangeColor, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Изменить цвет")
                    }
                }
                TextButton(onClick = onChangeIcon, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Изменить иконку")
                    }
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                        Text(if (folder.isPinned) "Открепить" else "Закрепить")
                    }
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                        Text("Удалить", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun RenameFolderDialog(folder: Folder, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название папки") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name) }, enabled = name.isNotBlank()) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun FolderColorDialog(currentColor: FolderColor, onDismiss: () -> Unit, onColorSelected: (FolderColor) -> Unit) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет папки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Пастельные", fontSize = 12.sp, color = Color(0xFF888888))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    FolderColor.entries.filter { it.name.startsWith("PASTEL") || it == FolderColor.BROWN }.forEach { color ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { selectedColor = color }) {
                                if (color == selectedColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Text("Яркие", fontSize = 12.sp, color = Color(0xFF888888))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    FolderColor.entries.filter { it.name.startsWith("VIBRANT") }.forEach { color ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { selectedColor = color }) {
                                if (color == selectedColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(selectedColor) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun FolderIconDialog(currentIcon: FolderIcon, onDismiss: () -> Unit, onIconSelected: (FolderIcon) -> Unit) {
    var selectedIcon by remember { mutableStateOf(currentIcon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Иконка папки") },
        text = {
            val icons = FolderIcon.entries.toList()
            Column {
                for (rowIdx in 0..(icons.size - 1) / 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (colIdx in 0 until 5) {
                            val idx = rowIdx * 5 + colIdx
                            if (idx < icons.size) {
                                val icon = icons[idx]
                                val isSelected = icon == selectedIcon
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFD2691E).copy(alpha = 0.2f) else Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { selectedIcon = icon }) {
                                        Icon(folderIconToVector(icon), null,
                                            tint = if (isSelected) Color(0xFF5D4037) else Color(0xFF888888),
                                            modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (rowIdx < (icons.size - 1) / 5) Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onIconSelected(selectedIcon) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
