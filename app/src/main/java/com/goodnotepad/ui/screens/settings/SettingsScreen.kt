package com.goodnotepad.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodnotepad.data.SortMode
import com.goodnotepad.data.ViewMode
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle
import com.goodnotepad.ui.screens.home.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf("Системная") }
    var selectedLanguage by remember { mutableStateOf("Русский") }
    var defaultFontSize by remember { mutableFloatStateOf(14f) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    var showClearTrashDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontWeight = FontWeight.Bold,
                        color = AppTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = AppTitle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppHeader)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Section: Appearance
            SettingsSection(title = "Внешний вид") {
                SettingsItem(
                    title = "Тема приложения",
                    subtitle = selectedTheme,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = "Язык",
                    subtitle = selectedLanguage,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            // Section: Notes
            SettingsSection(title = "Заметки") {
                SettingsItem(
                    title = "Сортировка по умолчанию",
                    subtitle = sortMode.label,
                    icon = Icons.Default.Sort,
                    onClick = { showSortDialog = true }
                )
                SettingsItem(
                    title = "Вид по умолчанию",
                    subtitle = when (viewMode) {
                        ViewMode.LIST -> "Список"
                        ViewMode.GRID_2 -> "Сетка 2x2"
                        ViewMode.GRID_3 -> "Сетка 3x3"
                        ViewMode.GRID_4 -> "Сетка 4x4"
                        ViewMode.GRID_5 -> "Сетка 5x5"
                    },
                    icon = Icons.Default.ViewModule,
                    onClick = { showViewDialog = true }
                )

                // Font size slider
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = Color(0xFF8B6914),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Размер шрифта по умолчанию",
                                fontSize = 16.sp,
                                color = Color(0xFF333333)
                            )
                            Text(
                                "${defaultFontSize.toInt()} sp",
                                fontSize = 14.sp,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                    Slider(
                        value = defaultFontSize,
                        onValueChange = { defaultFontSize = it },
                        valueRange = 12f..24f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD2691E),
                            activeTrackColor = Color(0xFFD2691E)
                        ),
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }

            // Section: Data
            SettingsSection(title = "Данные") {
                SettingsItem(
                    title = "Экспорт всех заметок",
                    subtitle = "Сохранить в .txt файл",
                    icon = Icons.Default.FileDownload,
                    onClick = { /* TODO: Export */ }
                )
                SettingsItem(
                    title = "Очистить корзину",
                    subtitle = "Удалить все заметки из корзины",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showClearTrashDialog = true },
                    isDestructive = true
                )
                SettingsItem(
                    title = "Сбросить все настройки",
                    subtitle = "Вернуть настройки по умолчанию",
                    icon = Icons.Default.RestartAlt,
                    onClick = { showResetDialog = true },
                    isDestructive = true
                )
            }

            // Section: About
            SettingsSection(title = "О приложении") {
                SettingsItem(
                    title = "Версия",
                    subtitle = "4.0",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
                SettingsItem(
                    title = "Разработчик",
                    subtitle = "GoodNotepad Team",
                    icon = Icons.Default.Person,
                    onClick = {}
                )
                SettingsItem(
                    title = "Политика конфиденциальности",
                    subtitle = "",
                    icon = Icons.Default.Policy,
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Theme dialog
    if (showThemeDialog) {
        val themes = listOf("Светлая", "Тёмная", "Системная")
        ListSelectionDialog(
            title = "Тема приложения",
            options = themes,
            selected = selectedTheme,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                selectedTheme = it
                showThemeDialog = false
            }
        )
    }

    // Language dialog
    if (showLanguageDialog) {
        val languages = listOf("Русский", "English")
        ListSelectionDialog(
            title = "Язык",
            options = languages,
            selected = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                selectedLanguage = it
                showLanguageDialog = false
            }
        )
    }

    // Sort dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Сортировка по умолчанию") },
            text = {
                Column {
                    SortMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = mode == sortMode,
                                onClick = {
                                    viewModel.setSortMode(mode)
                                    showSortDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // View mode dialog
    if (showViewDialog) {
        val modes = listOf(
            ViewMode.LIST to "Список",
            ViewMode.GRID_2 to "Сетка 2x2",
            ViewMode.GRID_3 to "Сетка 3x3",
            ViewMode.GRID_4 to "Сетка 4x4",
            ViewMode.GRID_5 to "Сетка 5x5"
        )
        AlertDialog(
            onDismissRequest = { showViewDialog = false },
            title = { Text("Вид по умолчанию") },
            text = {
                Column {
                    modes.forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = mode == viewMode,
                                onClick = {
                                    viewModel.setViewMode(mode)
                                    showViewDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Clear trash dialog
    if (showClearTrashDialog) {
        AlertDialog(
            onDismissRequest = { showClearTrashDialog = false },
            title = { Text("Очистить корзину?") },
            text = { Text("Все заметки из корзины будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showClearTrashDialog = false
                }) {
                    Text("Очистить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTrashDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Reset dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Все настройки будут возвращены к значениям по умолчанию.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedTheme = "Системная"
                    selectedLanguage = "Русский"
                    defaultFontSize = 14f
                    viewModel.setSortMode(SortMode.UPDATED_DESC)
                    viewModel.setViewMode(ViewMode.GRID_2)
                    showResetDialog = false
                }) {
                    Text("Сбросить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("О приложении") },
            text = {
                Column {
                    Text("Хороший Блокнот", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Версия: 4.0")
                    Text("Удобный блокнот для заметок")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GoodNotepad Team", color = Color(0xFF888888))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF8B6914),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0x20000000)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color.Red else Color(0xFF8B6914),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = if (isDestructive) Color.Red else Color(0xFF333333)
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ListSelectionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
