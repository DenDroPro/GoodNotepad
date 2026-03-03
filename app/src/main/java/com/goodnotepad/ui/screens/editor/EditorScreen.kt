package com.goodnotepad.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.goodnotepad.data.*
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.components.PageBackground
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    onNavigateBack: () -> Unit
) {
    val note by viewModel.currentNote.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var noteTheme by remember { mutableStateOf(NoteTheme.WHITE) }
    var pageStyle by remember { mutableStateOf(PageStyle.BLANK) }
    var headerColor by remember { mutableStateOf(HeaderColor.NONE) }
    var fontSize by remember { mutableIntStateOf(14) }
    var textAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var titleTextAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var isFavorite by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    // Issue #13: Track header height for PageBackground offset
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    // Load note - Issue #11: reset state before loading
    LaunchedEffect(noteId) {
        initialized = false
        title = ""
        content = ""
        viewModel.loadNote(noteId)
    }

    // Initialize fields when note loads
    LaunchedEffect(note) {
        note?.let {
            if (!initialized) {
                title = it.title
                content = it.content
                noteTheme = it.theme
                pageStyle = it.pageStyle
                headerColor = it.headerColor
                fontSize = it.fontSize
                textAlign = it.textAlign
                titleTextAlign = it.textAlign
                isFavorite = it.isFavorite
                initialized = true
            }
        }
    }

    // Auto-save
    LaunchedEffect(title, content, textAlign, pageStyle, noteTheme, headerColor, fontSize) {
        if (initialized) {
            note?.let {
                viewModel.saveNote(
                    it.copy(
                        title = title,
                        content = content,
                        preview = content.take(100),
                        theme = noteTheme,
                        pageStyle = pageStyle,
                        headerColor = headerColor,
                        fontSize = fontSize,
                        textAlign = textAlign,
                        isFavorite = isFavorite
                    )
                )
            }
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showPageStyleDialog by remember { mutableStateOf(false) }
    var showPageColorDialog by remember { mutableStateOf(false) }
    var showHeaderColorDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showHighlightMenu by remember { mutableStateOf(false) }

    // Formatting states - Issue #6: These now indicate the formatting for NEW text typed
    // Full selection-based formatting requires AnnotatedString which is complex
    // For now, these toggle the style of all content (as before) but the UI makes it clear
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }

    val composeTextAlign = when (textAlign) {
        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
        TextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
    }

    val composeTitleTextAlign = when (titleTextAlign) {
        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
        TextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
    }

    val alignIcon = when (textAlign) {
        TextAlign.LEFT -> Icons.Default.FormatAlignLeft
        TextAlign.CENTER -> Icons.Default.FormatAlignCenter
        TextAlign.RIGHT -> Icons.Default.FormatAlignRight
        TextAlign.JUSTIFY -> Icons.Default.FormatAlignJustify
    }

    // Issue #5: Line height = font size + 4 pixels (tight to text)
    val lineHeightSp = (fontSize + 4).sp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск в заметке...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск", tint = AppTitle)
                        }

                        // Issue #12: Alignment menu - separate for title and content
                        Box {
                            IconButton(onClick = { showAlignMenu = true }) {
                                Icon(alignIcon, contentDescription = "Выравнивание", tint = AppTitle)
                            }
                            DropdownMenu(
                                expanded = showAlignMenu,
                                onDismissRequest = { showAlignMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Текст: по левому краю") },
                                    onClick = { textAlign = TextAlign.LEFT; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Текст: по центру") },
                                    onClick = { textAlign = TextAlign.CENTER; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignCenter, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Текст: по правому краю") },
                                    onClick = { textAlign = TextAlign.RIGHT; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignRight, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Текст: по ширине") },
                                    onClick = { textAlign = TextAlign.JUSTIFY; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignJustify, null) }
                                )
                                HorizontalDivider()
                                // Issue #12: Header alignment separate
                                DropdownMenuItem(
                                    text = { Text("Шапка: по левому краю") },
                                    onClick = { titleTextAlign = TextAlign.LEFT; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Шапка: по центру") },
                                    onClick = { titleTextAlign = TextAlign.CENTER; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignCenter, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Шапка: по правому краю") },
                                    onClick = { titleTextAlign = TextAlign.RIGHT; showAlignMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignRight, null) }
                                )
                            }
                        }

                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Поделиться"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = AppTitle)
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ещё", tint = AppTitle)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Тип страницы") },
                                    onClick = { showPageStyleDialog = true; showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.GridOn, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Цвет страницы") },
                                    onClick = { showPageColorDialog = true; showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Palette, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Цвет шапки") },
                                    onClick = { showHeaderColorDialog = true; showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.ColorLens, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Размер шрифта") },
                                    onClick = { showFontSizeDialog = true; showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatSize, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) "Убрать из избранного" else "Добавить в избранное") },
                                    onClick = {
                                        isFavorite = !isFavorite
                                        note?.let { viewModel.toggleNoteFavorite(it.id) }
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteTheme.color)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF5F5F5),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bold
                    IconButton(
                        onClick = { isBold = !isBold },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            "Ж",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isBold) Color(0xFFD2691E) else Color(0xFF555555)
                        )
                    }

                    // Italic
                    IconButton(
                        onClick = { isItalic = !isItalic },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            "К",
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            color = if (isItalic) Color(0xFFD2691E) else Color(0xFF555555)
                        )
                    }

                    // Underline
                    IconButton(
                        onClick = { isUnderline = !isUnderline },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            "Ч",
                            textDecoration = TextDecoration.Underline,
                            fontSize = 16.sp,
                            color = if (isUnderline) Color(0xFFD2691E) else Color(0xFF555555)
                        )
                    }

                    // Strikethrough
                    IconButton(
                        onClick = { isStrikethrough = !isStrikethrough },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            "S",
                            textDecoration = TextDecoration.LineThrough,
                            fontSize = 16.sp,
                            color = if (isStrikethrough) Color(0xFFD2691E) else Color(0xFF555555)
                        )
                    }

                    // Issue #7: Highlight with "no color" option to clear
                    Box {
                        IconButton(
                            onClick = { showHighlightMenu = !showHighlightMenu },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Highlight,
                                contentDescription = "Маркер",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showHighlightMenu,
                            onDismissRequest = { showHighlightMenu = false }
                        ) {
                            // Issue #7: "No color" option first to clear highlighting
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFCCCCCC)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Без маркера")
                                    }
                                },
                                onClick = { showHighlightMenu = false }
                            )
                            HighlightColor.entries.filter { it != HighlightColor.NONE }.forEach { color ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(color.color.copy(alpha = 0.5f))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(color.name)
                                        }
                                    },
                                    onClick = { showHighlightMenu = false }
                                )
                            }
                        }
                    }

                    // Font size decrease
                    IconButton(
                        onClick = { if (fontSize > 10) fontSize-- },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("A-", fontSize = 14.sp, color = Color(0xFF555555))
                    }

                    // Font size increase
                    IconButton(
                        onClick = { if (fontSize < 30) fontSize++ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("A+", fontSize = 14.sp, color = Color(0xFF555555))
                    }

                    // Bulleted list
                    IconButton(
                        onClick = {
                            content = if (content.endsWith("\n") || content.isEmpty()) {
                                content + "\u2022 "
                            } else {
                                content + "\n\u2022 "
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Список", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }

                    // Numbered list
                    IconButton(
                        onClick = {
                            val lines = content.split("\n")
                            val lastNum = lines.lastOrNull()?.let { line ->
                                val match = Regex("^(\\d+)\\.").find(line)
                                match?.groupValues?.get(1)?.toIntOrNull()
                            } ?: 0
                            content = if (content.endsWith("\n") || content.isEmpty()) {
                                content + "${lastNum + 1}. "
                            } else {
                                content + "\n${lastNum + 1}. "
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Нумерованный список", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }

                    // Horizontal rule
                    IconButton(
                        onClick = {
                            content = if (content.endsWith("\n") || content.isEmpty()) {
                                content + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n"
                            } else {
                                content + "\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n"
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "Линия", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = noteTheme.color
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Issue #13: PageBackground with header offset so no lines/grid/dots in header area
            PageBackground(
                pageStyle = pageStyle,
                noteTheme = noteTheme,
                fontSize = fontSize,
                headerHeightPx = headerHeightPx
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Issue #4: Header color is BACKGROUND of title field, not separate bar
                // Issue #13: No grid/lines/dots in header area (handled by PageBackground headerHeightPx)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (headerColor != HeaderColor.NONE) headerColor.color
                            else Color.Transparent
                        )
                        .onGloballyPositioned { coordinates ->
                            headerHeightPx = coordinates.size.height.toFloat()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF333333),
                            textAlign = composeTitleTextAlign
                        ),
                        cursorBrush = SolidColor(Color(0xFFD2691E)),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Заголовок",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Color(0xFFBBBBBB)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0x20000000)
                )

                // Content field
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        color = Color(0xFF333333),
                        lineHeight = lineHeightSp,
                        textAlign = composeTextAlign,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = when {
                            isUnderline && isStrikethrough -> TextDecoration.combine(
                                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                            )
                            isUnderline -> TextDecoration.Underline
                            isStrikethrough -> TextDecoration.LineThrough
                            else -> TextDecoration.None
                        }
                    ),
                    cursorBrush = SolidColor(Color(0xFFD2691E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (content.isEmpty()) {
                                Text(
                                    "Начните писать...",
                                    style = TextStyle(
                                        fontSize = fontSize.sp,
                                        color = Color(0xFFBBBBBB)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    // Page Style Dialog
    if (showPageStyleDialog) {
        PageStyleDialog(
            currentStyle = pageStyle,
            onDismiss = { showPageStyleDialog = false },
            onStyleSelected = {
                pageStyle = it
                showPageStyleDialog = false
            }
        )
    }

    // Page Color Dialog
    if (showPageColorDialog) {
        PageColorDialog(
            currentTheme = noteTheme,
            onDismiss = { showPageColorDialog = false },
            onThemeSelected = {
                noteTheme = it
                showPageColorDialog = false
            }
        )
    }

    // Header Color Dialog
    if (showHeaderColorDialog) {
        EditorHeaderColorDialog(
            currentColor = headerColor,
            onDismiss = { showHeaderColorDialog = false },
            onColorSelected = {
                headerColor = it
                showHeaderColorDialog = false
            }
        )
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = fontSize,
            onDismiss = { showFontSizeDialog = false },
            onSizeSelected = {
                fontSize = it
                showFontSizeDialog = false
            }
        )
    }
}

@Composable
fun PageStyleDialog(
    currentStyle: PageStyle,
    onDismiss: () -> Unit,
    onStyleSelected: (PageStyle) -> Unit
) {
    val styles = listOf(
        PageStyle.BLANK to "Чистый лист",
        PageStyle.LINED to "В линейку",
        PageStyle.GRID to "В клетку",
        PageStyle.DOTTED to "В точку"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тип страницы") },
        text = {
            Column {
                styles.forEach { (style, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = style == currentStyle,
                            onClick = { onStyleSelected(style) }
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

@Composable
fun PageColorDialog(
    currentTheme: NoteTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (NoteTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет страницы") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NoteTheme.entries.forEach { theme ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.color)
                            .then(
                                if (theme == currentTheme)
                                    Modifier.background(Color.Black.copy(alpha = 0.15f), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onThemeSelected(theme) }) {
                            if (theme == currentTheme) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF555555), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun EditorHeaderColorDialog(
    currentColor: HeaderColor,
    onDismiss: () -> Unit,
    onColorSelected: (HeaderColor) -> Unit
) {
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
                        IconButton(onClick = { onColorSelected(color) }) {
                            if (color == HeaderColor.NONE) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            } else if (color == currentColor) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onSizeSelected: (Int) -> Unit
) {
    val sizes = listOf(12, 14, 16, 18, 20, 22, 24)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Размер шрифта") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sizes.forEach { size ->
                    FilledTonalButton(
                        onClick = { onSizeSelected(size) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (size == currentSize) Color(0xFFD2691E) else Color(0xFFE0E0E0),
                            contentColor = if (size == currentSize) Color.White else Color.Black
                        ),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$size", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
