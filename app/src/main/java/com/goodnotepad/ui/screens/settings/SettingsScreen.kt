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
    var selectedTheme by remember { mutableStateOf("\u0421\u0438\u0441\u0442\u0435\u043c\u043d\u0430\u044f") }
    var selectedLanguage by remember { mutableStateOf("\u0420\u0443\u0441\u0441\u043a\u0438\u0439") }
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
    val lineOpacity by viewModel.lineOpacity.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438",
                        fontWeight = FontWeight.Bold,
                        color = AppTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "\u041d\u0430\u0437\u0430\u0434", tint = AppTitle)
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
            SettingsSection(title = "\u0412\u043d\u0435\u0448\u043d\u0438\u0439 \u0432\u0438\u0434") {
                SettingsItem(
                    title = "\u0422\u0435\u043c\u0430 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f",
                    subtitle = selectedTheme,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = "\u042f\u0437\u044b\u043a",
                    subtitle = selectedLanguage,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            SettingsSection(title = "\u0417\u0430\u043c\u0435\u0442\u043a\u0438") {
                SettingsItem(
                    title = "\u0421\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e",
                    subtitle = sortMode.label,
                    icon = Icons.Default.Sort,
                    onClick = { showSortDialog = true }
                )
                SettingsItem(
                    title = "\u0412\u0438\u0434 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e",
                    subtitle = when (viewMode) {
                        ViewMode.LIST -> "\u0421\u043f\u0438\u0441\u043e\u043a"
                        ViewMode.GRID_2 -> "\u0421\u0435\u0442\u043a\u0430 2x2"
                        ViewMode.GRID_3 -> "\u0421\u0435\u0442\u043a\u0430 3x3"
                        ViewMode.GRID_4 -> "\u0421\u0435\u0442\u043a\u0430 4x4"
                        ViewMode.GRID_5 -> "\u0421\u0435\u0442\u043a\u0430 5x5"
                    },
                    icon = Icons.Default.ViewModule,
                    onClick = { showViewDialog = true }
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FormatSize, null, tint = Color(0xFF8B6914), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("\u0420\u0430\u0437\u043c\u0435\u0440 \u0448\u0440\u0438\u0444\u0442\u0430 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e", fontSize = 16.sp, color = Color(0xFF333333))
                            Text("${defaultFontSize.toInt()} sp", fontSize = 14.sp, color = Color(0xFF888888))
                        }
                    }
                    Slider(
                        value = defaultFontSize,
                        onValueChange = { defaultFontSize = it },
                        valueRange = 12f..24f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)),
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }

                // Issue #11: Brightness control for lines/grid/dots
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Brightness6, null, tint = Color(0xFF8B6914), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u043b\u0438\u043d\u0435\u0435\u043a/\u043a\u043b\u0435\u0442\u043e\u043a/\u0442\u043e\u0447\u0435\u043a", fontSize = 16.sp, color = Color(0xFF333333))
                            Text("${(lineOpacity * 100).toInt()}%", fontSize = 14.sp, color = Color(0xFF888888))
                        }
                    }
                    Slider(
                        value = lineOpacity,
                        onValueChange = { viewModel.setLineOpacity(it) },
                        valueRange = 0.05f..0.5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)),
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }

            SettingsSection(title = "\u0414\u0430\u043d\u043d\u044b\u0435") {
                SettingsItem(
                    title = "\u042d\u043a\u0441\u043f\u043e\u0440\u0442 \u0432\u0441\u0435\u0445 \u0437\u0430\u043c\u0435\u0442\u043e\u043a",
                    subtitle = "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432 .txt \u0444\u0430\u0439\u043b",
                    icon = Icons.Default.FileDownload,
                    onClick = { }
                )
                SettingsItem(
                    title = "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u043a\u043e\u0440\u0437\u0438\u043d\u0443",
                    subtitle = "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0432\u0441\u0435 \u0437\u0430\u043c\u0435\u0442\u043a\u0438 \u0438\u0437 \u043a\u043e\u0440\u0437\u0438\u043d\u044b",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showClearTrashDialog = true },
                    isDestructive = true
                )
                SettingsItem(
                    title = "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u0432\u0441\u0435 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438",
                    subtitle = "\u0412\u0435\u0440\u043d\u0443\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e",
                    icon = Icons.Default.RestartAlt,
                    onClick = { showResetDialog = true },
                    isDestructive = true
                )
            }

            SettingsSection(title = "\u041e \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0438") {
                SettingsItem(
                    title = "\u0412\u0435\u0440\u0441\u0438\u044f",
                    subtitle = "5.0",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
                SettingsItem(
                    title = "\u0420\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a",
                    subtitle = "GoodNotepad Team",
                    icon = Icons.Default.Person,
                    onClick = {}
                )
                SettingsItem(
                    title = "\u041f\u043e\u043b\u0438\u0442\u0438\u043a\u0430 \u043a\u043e\u043d\u0444\u0438\u0434\u0435\u043d\u0446\u0438\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u0438",
                    subtitle = "",
                    icon = Icons.Default.Policy,
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showThemeDialog) {
        val themes = listOf("\u0421\u0432\u0435\u0442\u043b\u0430\u044f", "\u0422\u0451\u043c\u043d\u0430\u044f", "\u0421\u0438\u0441\u0442\u0435\u043c\u043d\u0430\u044f")
        ListSelectionDialog(
            title = "\u0422\u0435\u043c\u0430 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f",
            options = themes,
            selected = selectedTheme,
            onDismiss = { showThemeDialog = false },
            onSelect = { selectedTheme = it; showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        val languages = listOf("\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "English")
        ListSelectionDialog(
            title = "\u042f\u0437\u044b\u043a",
            options = languages,
            selected = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { selectedLanguage = it; showLanguageDialog = false }
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("\u0421\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e") },
            text = {
                Column {
                    SortMode.entries.forEach { mode ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = mode == sortMode, onClick = { viewModel.setSortMode(mode); showSortDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showViewDialog) {
        val modes = listOf(
            ViewMode.LIST to "\u0421\u043f\u0438\u0441\u043e\u043a",
            ViewMode.GRID_2 to "\u0421\u0435\u0442\u043a\u0430 2x2",
            ViewMode.GRID_3 to "\u0421\u0435\u0442\u043a\u0430 3x3",
            ViewMode.GRID_4 to "\u0421\u0435\u0442\u043a\u0430 4x4",
            ViewMode.GRID_5 to "\u0421\u0435\u0442\u043a\u0430 5x5"
        )
        AlertDialog(
            onDismissRequest = { showViewDialog = false },
            title = { Text("\u0412\u0438\u0434 \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e") },
            text = {
                Column {
                    modes.forEach { (mode, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = mode == viewMode, onClick = { viewModel.setViewMode(mode); showViewDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showClearTrashDialog) {
        AlertDialog(
            onDismissRequest = { showClearTrashDialog = false },
            title = { Text("\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u043a\u043e\u0440\u0437\u0438\u043d\u0443?") },
            text = { Text("\u0412\u0441\u0435 \u0437\u0430\u043c\u0435\u0442\u043a\u0438 \u0438\u0437 \u043a\u043e\u0440\u0437\u0438\u043d\u044b \u0431\u0443\u0434\u0443\u0442 \u0443\u0434\u0430\u043b\u0435\u043d\u044b \u0431\u0435\u0437\u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043d\u043e.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash(); showClearTrashDialog = false }) {
                    Text("\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTrashDialog = false }) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438?") },
            text = { Text("\u0412\u0441\u0435 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0431\u0443\u0434\u0443\u0442 \u0432\u043e\u0437\u0432\u0440\u0430\u0449\u0435\u043d\u044b \u043a \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f\u043c \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedTheme = "\u0421\u0438\u0441\u0442\u0435\u043c\u043d\u0430\u044f"
                    selectedLanguage = "\u0420\u0443\u0441\u0441\u043a\u0438\u0439"
                    defaultFontSize = 14f
                    viewModel.setSortMode(SortMode.UPDATED_DESC)
                    viewModel.setViewMode(ViewMode.GRID_2)
                    viewModel.setLineOpacity(0.15f)
                    showResetDialog = false
                }) {
                    Text("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("\u041e\u0442\u043c\u0435\u043d\u0430") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("\u041e \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0438") },
            text = {
                Column {
                    Text("\u0425\u043e\u0440\u043e\u0448\u0438\u0439 \u0411\u043b\u043e\u043a\u043d\u043e\u0442", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u0412\u0435\u0440\u0441\u0438\u044f: 5.0")
                    Text("\u0423\u0434\u043e\u0431\u043d\u044b\u0439 \u0431\u043b\u043e\u043a\u043d\u043e\u0442 \u0434\u043b\u044f \u0437\u0430\u043c\u0435\u0442\u043e\u043a")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GoodNotepad Team", color = Color(0xFF888888))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c") }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF8B6914), modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0x20000000))
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, isDestructive: Boolean = false) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isDestructive) Color.Red else Color(0xFF8B6914), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = if (isDestructive) Color.Red else Color(0xFF333333))
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 14.sp, color = Color(0xFF888888))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ListSelectionDialog(title: String, options: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
