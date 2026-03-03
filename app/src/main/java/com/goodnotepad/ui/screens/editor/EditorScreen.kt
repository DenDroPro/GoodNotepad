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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.goodnotepad.data.*
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Per-character formatting info */
data class CharFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val highlightColor: HighlightColor = HighlightColor.NONE
) {
    fun toSpanStyle(): SpanStyle = SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = when {
            underline && strikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            underline -> TextDecoration.Underline
            strikethrough -> TextDecoration.LineThrough
            else -> null
        },
        background = if (highlightColor != HighlightColor.NONE) highlightColor.color.copy(alpha = 0.35f) else Color.Unspecified
    )
}

fun serializeFormats(formats: List<CharFormat>): String {
    if (formats.isEmpty() || formats.all { it == CharFormat() }) return ""
    val arr = JSONArray()
    val defaultFmt = CharFormat()
    var i = 0
    while (i < formats.size) {
        val fmt = formats[i]
        val start = i
        while (i < formats.size && formats[i] == fmt) i++
        if (fmt != defaultFmt) {
            val obj = JSONObject()
            obj.put("s", start)
            obj.put("e", i)
            if (fmt.bold) obj.put("b", true)
            if (fmt.italic) obj.put("i", true)
            if (fmt.underline) obj.put("u", true)
            if (fmt.strikethrough) obj.put("st", true)
            if (fmt.highlightColor != HighlightColor.NONE) obj.put("h", fmt.highlightColor.name)
            arr.put(obj)
        }
    }
    return if (arr.length() == 0) "" else arr.toString()
}

fun deserializeFormats(json: String, textLength: Int): List<CharFormat> {
    val result = MutableList(textLength) { CharFormat() }
    if (json.isBlank()) return result
    try {
        val arr = JSONArray(json)
        for (idx in 0 until arr.length()) {
            val obj = arr.getJSONObject(idx)
            val s = obj.getInt("s")
            val e = obj.getInt("e").coerceAtMost(textLength)
            val fmt = CharFormat(
                bold = obj.optBoolean("b", false),
                italic = obj.optBoolean("i", false),
                underline = obj.optBoolean("u", false),
                strikethrough = obj.optBoolean("st", false),
                highlightColor = try { HighlightColor.valueOf(obj.optString("h", "NONE")) } catch (_: Exception) { HighlightColor.NONE }
            )
            for (j in s until e) {
                if (j < textLength) result[j] = fmt
            }
        }
    } catch (_: Exception) {}
    return result
}

/**
 * Build AnnotatedString with per-char formatting AND ParagraphStyle for text alignment.
 * ParagraphStyle is the ONLY reliable way to align text in BasicTextField.
 */
fun buildFormattedString(
    text: String,
    formats: List<CharFormat>,
    paragraphTextAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        // Force text alignment via ParagraphStyle - this works even when BasicTextField ignores textStyle.textAlign
        if (text.isNotEmpty()) {
            addStyle(ParagraphStyle(textAlign = paragraphTextAlign), 0, text.length)
        }
        if (formats.isEmpty() || text.isEmpty()) return@buildAnnotatedString
        val defaultFmt = CharFormat()
        var i = 0
        while (i < text.length && i < formats.size) {
            val fmt = formats[i]
            val start = i
            while (i < text.length && i < formats.size && formats[i] == fmt) i++
            if (fmt != defaultFmt) {
                addStyle(fmt.toSpanStyle(), start, i)
            }
        }
    }
}

/**
 * Build AnnotatedString for title with ParagraphStyle alignment.
 */
fun buildTitleString(
    text: String,
    paragraphTextAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (text.isNotEmpty()) {
            addStyle(ParagraphStyle(textAlign = paragraphTextAlign), 0, text.length)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    onNavigateBack: () -> Unit
) {
    val note by viewModel.currentNote.collectAsState()
    val context = LocalContext.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    // State
    var titleText by remember { mutableStateOf("") }
    var titleSelection by remember { mutableStateOf(TextRange.Zero) }
    var contentText by remember { mutableStateOf("") }
    var contentSelection by remember { mutableStateOf(TextRange.Zero) }
    var contentComposition by remember { mutableStateOf<TextRange?>(null) }
    var noteTheme by remember { mutableStateOf(NoteTheme.WHITE) }
    var pageStyle by remember { mutableStateOf(PageStyle.BLANK) }
    var headerColor by remember { mutableStateOf(HeaderColor.NONE) }
    var fontSize by remember { mutableIntStateOf(14) }
    var textAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var titleTextAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var noteLineOpacity by remember { mutableFloatStateOf(0.3f) }
    var isFavorite by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    // Per-character formatting
    val charFormats = remember { mutableListOf<CharFormat>() }
    var formatVersion by remember { mutableIntStateOf(0) }
    var activeFormat by remember { mutableStateOf(CharFormat()) }

    // Text layout result for drawing lines
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Load note
    LaunchedEffect(noteId) {
        initialized = false
        titleText = ""
        contentText = ""
        charFormats.clear()
        formatVersion++
        viewModel.loadNote(noteId)
    }

    LaunchedEffect(note) {
        note?.let {
            if (!initialized) {
                titleText = it.title
                titleSelection = TextRange(it.title.length)
                contentText = it.content
                contentSelection = TextRange(it.content.length)
                charFormats.clear()
                charFormats.addAll(deserializeFormats(it.formatting, it.content.length))
                formatVersion++
                noteTheme = it.theme
                pageStyle = it.pageStyle
                headerColor = it.headerColor
                fontSize = it.fontSize
                textAlign = it.textAlign
                titleTextAlign = it.titleTextAlign
                noteLineOpacity = it.lineOpacity
                isFavorite = it.isFavorite
                initialized = true
            }
        }
    }

    // Auto-save
    LaunchedEffect(titleText, contentText, textAlign, titleTextAlign, pageStyle, noteTheme, headerColor, fontSize, noteLineOpacity, formatVersion) {
        if (initialized) {
            note?.let {
                viewModel.saveNote(it.copy(
                    title = titleText, content = contentText, preview = contentText.take(100),
                    formatting = serializeFormats(charFormats),
                    theme = noteTheme, pageStyle = pageStyle, headerColor = headerColor,
                    fontSize = fontSize, textAlign = textAlign, titleTextAlign = titleTextAlign,
                    lineOpacity = noteLineOpacity, isFavorite = isFavorite
                ))
            }
        }
    }

    // UI state
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showPageStyleDialog by remember { mutableStateOf(false) }
    var showPageColorDialog by remember { mutableStateOf(false) }
    var showHeaderColorDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showHighlightMenu by remember { mutableStateOf(false) }

    // Compose text alignment values
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

    val lineHeightSp = (fontSize * 1.5f).sp

    // Build title with ParagraphStyle alignment
    val titleAnnotated = remember(titleText, composeTitleTextAlign) {
        buildTitleString(titleText, composeTitleTextAlign)
    }
    val titleFieldValue = TextFieldValue(
        annotatedString = titleAnnotated,
        selection = titleSelection
    )

    // Build content with ParagraphStyle alignment + char formatting
    val annotatedContent = remember(contentText, formatVersion, composeTextAlign) {
        buildFormattedString(contentText, charFormats.toList(), composeTextAlign)
    }
    val displayValue = TextFieldValue(
        annotatedString = annotatedContent,
        selection = contentSelection,
        composition = contentComposition
    )

    // Title change handler
    fun onTitleChange(newValue: TextFieldValue) {
        titleText = newValue.text
        titleSelection = newValue.selection
    }

    // Content change handler
    fun onContentChange(newValue: TextFieldValue) {
        val oldText = contentText
        val newText = newValue.text
        if (newText.length > oldText.length) {
            val insertLen = newText.length - oldText.length
            val insertPos = (newValue.selection.start - insertLen).coerceIn(0, charFormats.size)
            repeat(insertLen) { charFormats.add(insertPos, activeFormat.copy()) }
        } else if (newText.length < oldText.length) {
            val deleteLen = oldText.length - newText.length
            val deletePos = newValue.selection.start.coerceIn(0, charFormats.size)
            repeat(deleteLen) { if (deletePos < charFormats.size) charFormats.removeAt(deletePos) }
        }
        contentText = newText
        contentSelection = newValue.selection
        contentComposition = newValue.composition
        formatVersion++
    }

    // Format toggle helpers
    fun toggleFmt(getter: (CharFormat) -> Boolean, setter: (CharFormat, Boolean) -> CharFormat) {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) {
            val end = sel.max.coerceAtMost(charFormats.size)
            val allHave = (sel.min until end).all { getter(charFormats[it]) }
            for (i in sel.min until end) { charFormats[i] = setter(charFormats[i], !allHave) }
            formatVersion++
        } else {
            activeFormat = setter(activeFormat, !getter(activeFormat))
        }
    }
    fun toggleBold() = toggleFmt({ it.bold }, { f, v -> f.copy(bold = v) })
    fun toggleItalic() = toggleFmt({ it.italic }, { f, v -> f.copy(italic = v) })
    fun toggleUnderline() = toggleFmt({ it.underline }, { f, v -> f.copy(underline = v) })
    fun toggleStrikethrough() = toggleFmt({ it.strikethrough }, { f, v -> f.copy(strikethrough = v) })

    fun applyHighlight(color: HighlightColor) {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) {
            val end = sel.max.coerceAtMost(charFormats.size)
            for (i in sel.min until end) { charFormats[i] = charFormats[i].copy(highlightColor = color) }
            formatVersion++
        }
    }

    // Button active states
    val isBoldActive = run {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.bold == true }
        else activeFormat.bold
    }
    val isItalicActive = run {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.italic == true }
        else activeFormat.italic
    }
    val isUnderlineActive = run {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.underline == true }
        else activeFormat.underline
    }
    val isStrikethroughActive = run {
        val sel = contentSelection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.strikethrough == true }
        else activeFormat.strikethrough
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("\u041f\u043e\u0438\u0441\u043a \u0432 \u0437\u0430\u043c\u0435\u0442\u043a\u0435...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth())
                    }
                },
                navigationIcon = {
                    if (showSearch) {
                        IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "\u0417\u0430\u043a\u0440\u044b\u0442\u044c")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "\u041d\u0430\u0437\u0430\u0434", tint = AppTitle, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "\u041f\u043e\u0438\u0441\u043a", tint = AppTitle, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            IconButton(onClick = { showAlignMenu = true }, modifier = Modifier.size(40.dp)) {
                                Icon(alignIcon, contentDescription = "\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435", tint = AppTitle, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showAlignMenu, onDismissRequest = { showAlignMenu = false }) {
                                Text(
                                    "\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435 \u0448\u0430\u043f\u043a\u0438",
                                    fontSize = 12.sp, color = Color(0xFF888888),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = { titleTextAlign = TextAlign.LEFT; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignLeft, null, tint = if (titleTextAlign == TextAlign.LEFT) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { titleTextAlign = TextAlign.CENTER; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignCenter, null, tint = if (titleTextAlign == TextAlign.CENTER) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { titleTextAlign = TextAlign.RIGHT; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignRight, null, tint = if (titleTextAlign == TextAlign.RIGHT) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { titleTextAlign = TextAlign.JUSTIFY; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignJustify, null, tint = if (titleTextAlign == TextAlign.JUSTIFY) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u043a\u0441\u0442\u0430",
                                    fontSize = 12.sp, color = Color(0xFF888888),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = { textAlign = TextAlign.LEFT; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignLeft, null, tint = if (textAlign == TextAlign.LEFT) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { textAlign = TextAlign.CENTER; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignCenter, null, tint = if (textAlign == TextAlign.CENTER) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { textAlign = TextAlign.RIGHT; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignRight, null, tint = if (textAlign == TextAlign.RIGHT) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                    IconButton(onClick = { textAlign = TextAlign.JUSTIFY; showAlignMenu = false }) {
                                        Icon(Icons.Default.FormatAlignJustify, null, tint = if (textAlign == TextAlign.JUSTIFY) Color(0xFFD2691E) else Color(0xFF666666))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        IconButton(onClick = {
                            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "$titleText\n\n$contentText"); type = "text/plain" }
                            context.startActivity(Intent.createChooser(sendIntent, "\u041f\u043e\u0434\u0435\u043b\u0438\u0442\u044c\u0441\u044f"))
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "\u041f\u043e\u0434\u0435\u043b\u0438\u0442\u044c\u0441\u044f", tint = AppTitle, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "\u0415\u0449\u0451", tint = AppTitle, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(text = { Text("\u0422\u0438\u043f \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b") }, onClick = { showPageStyleDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.GridOn, null) })
                                DropdownMenuItem(text = { Text("\u0426\u0432\u0435\u0442 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b") }, onClick = { showPageColorDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.Palette, null) })
                                DropdownMenuItem(text = { Text("\u0426\u0432\u0435\u0442 \u0448\u0430\u043f\u043a\u0438") }, onClick = { showHeaderColorDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.ColorLens, null) })
                                HorizontalDivider()
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("\u0420\u0430\u0437\u043c\u0435\u0440 \u0448\u0440\u0438\u0444\u0442\u0430: ${fontSize}sp", fontSize = 13.sp, color = Color(0xFF555555))
                                    Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.roundToInt() }, valueRange = 10f..30f, steps = 19, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)))
                                }
                                HorizontalDivider()
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u043b\u0438\u043d\u0435\u0435\u043a: ${(noteLineOpacity * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF555555))
                                    Slider(value = noteLineOpacity, onValueChange = { noteLineOpacity = it }, valueRange = 0.05f..0.5f, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)))
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) "\u0423\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0433\u043e" else "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435") },
                                    onClick = { isFavorite = !isFavorite; note?.let { viewModel.toggleNoteFavorite(it.id) }; showMoreMenu = false },
                                    leadingIcon = { Icon(if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteTheme.color)
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F5), shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { toggleBold() }, modifier = Modifier.size(40.dp)) {
                        Text("\u0416", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isBoldActive) Color(0xFFD2691E) else Color(0xFF555555))
                    }
                    IconButton(onClick = { toggleItalic() }, modifier = Modifier.size(40.dp)) {
                        Text("\u041a", fontStyle = FontStyle.Italic, fontSize = 16.sp, color = if (isItalicActive) Color(0xFFD2691E) else Color(0xFF555555))
                    }
                    IconButton(onClick = { toggleUnderline() }, modifier = Modifier.size(40.dp)) {
                        Text("\u0427", textDecoration = TextDecoration.Underline, fontSize = 16.sp, color = if (isUnderlineActive) Color(0xFFD2691E) else Color(0xFF555555))
                    }
                    IconButton(onClick = { toggleStrikethrough() }, modifier = Modifier.size(40.dp)) {
                        Text("S", textDecoration = TextDecoration.LineThrough, fontSize = 16.sp, color = if (isStrikethroughActive) Color(0xFFD2691E) else Color(0xFF555555))
                    }
                    Box {
                        IconButton(onClick = { showHighlightMenu = !showHighlightMenu }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Highlight, contentDescription = "\u041c\u0430\u0440\u043a\u0435\u0440", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showHighlightMenu, onDismissRequest = { showHighlightMenu = false }) {
                            DropdownMenuItem(
                                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFCCCCCC)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }; Spacer(Modifier.width(8.dp)); Text("\u0411\u0435\u0437 \u043c\u0430\u0440\u043a\u0435\u0440\u0430")
                                } },
                                onClick = { applyHighlight(HighlightColor.NONE); showHighlightMenu = false }
                            )
                            HighlightColor.entries.filter { it != HighlightColor.NONE }.forEach { color ->
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(24.dp).clip(CircleShape).background(color.color.copy(alpha = 0.5f)))
                                        Spacer(Modifier.width(8.dp)); Text(color.name)
                                    } },
                                    onClick = { applyHighlight(color); showHighlightMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { if (fontSize > 10) fontSize-- }, modifier = Modifier.size(40.dp)) {
                        Text("A-", fontSize = 14.sp, color = Color(0xFF555555))
                    }
                    IconButton(onClick = { if (fontSize < 30) fontSize++ }, modifier = Modifier.size(40.dp)) {
                        Text("A+", fontSize = 14.sp, color = Color(0xFF555555))
                    }
                    IconButton(onClick = {
                        val nl = if (contentText.endsWith("\n") || contentText.isEmpty()) "" else "\n"
                        val ins = nl + "\u2022 "
                        val pos = contentSelection.start.coerceIn(0, contentText.length)
                        contentText = contentText.substring(0, pos) + ins + contentText.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentSelection = TextRange(pos + ins.length)
                        formatVersion++
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "\u0421\u043f\u0438\u0441\u043e\u043a", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val pos = contentSelection.start.coerceIn(0, contentText.length)
                        val textBefore = contentText.substring(0, pos)
                        val paragraphLines = textBefore.split("\n")
                        var lastNum = 0
                        for (i in paragraphLines.indices.reversed()) {
                            val line = paragraphLines[i].trim()
                            if (line.isEmpty()) break
                            val match = Regex("^(\\d+)\\.").find(line)
                            if (match != null) {
                                lastNum = match.groupValues[1].toIntOrNull() ?: 0
                                break
                            }
                        }
                        val nl = if (textBefore.endsWith("\n") || contentText.isEmpty()) "" else "\n"
                        val ins = nl + "${lastNum + 1}. "
                        contentText = contentText.substring(0, pos) + ins + contentText.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentSelection = TextRange(pos + ins.length)
                        formatVersion++
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "\u041d\u0443\u043c\u0435\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0439", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val nl = if (contentText.endsWith("\n") || contentText.isEmpty()) "" else "\n"
                        val ins = nl + "\u2500".repeat(16) + "\n"
                        val pos = contentSelection.start.coerceIn(0, contentText.length)
                        contentText = contentText.substring(0, pos) + ins + contentText.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentSelection = TextRange(pos + ins.length)
                        formatVersion++
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "\u041b\u0438\u043d\u0438\u044f", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = noteTheme.color
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header area - clean, no lines
            // Padding on Box for visual spacing, BasicTextField fills full Box width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (headerColor != HeaderColor.NONE) headerColor.color else Color.Transparent)
                    .padding(horizontal = 15.dp, vertical = 12.dp)
            ) {
                // Use TextFieldValue with ParagraphStyle for reliable alignment
                BasicTextField(
                    value = titleFieldValue,
                    onValueChange = { onTitleChange(it) },
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = Color(0xFF333333)
                    ),
                    cursorBrush = SolidColor(Color(0xFFD2691E)),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (titleText.isEmpty()) {
                                Text(
                                    "\u0417\u0430\u0433\u043e\u043b\u043e\u0432\u043e\u043a",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold, fontSize = 22.sp,
                                        color = Color(0xFFBBBBBB), textAlign = composeTitleTextAlign
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 15.dp), color = Color(0x20000000))

            // Content area with lines drawn behind
            // 15dp horizontal padding for text, lines go edge-to-edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeightDp.dp)
            ) {
                // Line drawing layer - full width, behind text
                val lineColor = Color.Black.copy(alpha = noteLineOpacity)
                val currentTextLayout = textLayoutResult
                val currentPageStyle = pageStyle
                val lhSp = lineHeightSp
                val padHorizontal = 15.dp

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val lhPx = lhSp.toPx()
                            val padTopPx = 8.dp.toPx()

                            if (currentTextLayout != null && currentTextLayout.lineCount > 0) {
                                when (currentPageStyle) {
                                    PageStyle.LINED -> {
                                        for (i in 0 until currentTextLayout.lineCount) {
                                            val lineBottom = currentTextLayout.getLineBottom(i) + padTopPx
                                            drawLine(lineColor, Offset(0f, lineBottom), Offset(size.width, lineBottom), strokeWidth = 0.8f)
                                        }
                                        val lastBottom = currentTextLayout.getLineBottom(currentTextLayout.lineCount - 1) + padTopPx
                                        var y = lastBottom + lhPx
                                        while (y < size.height) {
                                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                                            y += lhPx
                                        }
                                    }
                                    PageStyle.GRID -> {
                                        for (i in 0 until currentTextLayout.lineCount) {
                                            val lineBottom = currentTextLayout.getLineBottom(i) + padTopPx
                                            drawLine(lineColor, Offset(0f, lineBottom), Offset(size.width, lineBottom), strokeWidth = 0.5f)
                                        }
                                        val lastBottom = currentTextLayout.getLineBottom(currentTextLayout.lineCount - 1) + padTopPx
                                        var y = lastBottom + lhPx
                                        while (y < size.height) {
                                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                                            y += lhPx
                                        }
                                        var x = lhPx
                                        while (x < size.width) {
                                            drawLine(lineColor, Offset(x, padTopPx), Offset(x, size.height), strokeWidth = 0.5f)
                                            x += lhPx
                                        }
                                    }
                                    PageStyle.DOTTED -> {
                                        val dotColor = Color.Black.copy(alpha = (noteLineOpacity * 1.5f).coerceAtMost(1f))
                                        for (i in 0 until currentTextLayout.lineCount) {
                                            val lineBottom = currentTextLayout.getLineBottom(i) + padTopPx
                                            var x = lhPx
                                            while (x < size.width) {
                                                drawCircle(dotColor, radius = 1.5f, center = Offset(x, lineBottom))
                                                x += lhPx
                                            }
                                        }
                                        val lastBottom = currentTextLayout.getLineBottom(currentTextLayout.lineCount - 1) + padTopPx
                                        var y = lastBottom + lhPx
                                        while (y < size.height) {
                                            var x = lhPx
                                            while (x < size.width) {
                                                drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                                                x += lhPx
                                            }
                                            y += lhPx
                                        }
                                    }
                                    PageStyle.BLANK -> {}
                                }
                            } else {
                                when (currentPageStyle) {
                                    PageStyle.LINED -> {
                                        var y = padTopPx + lhPx
                                        while (y < size.height) {
                                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                                            y += lhPx
                                        }
                                    }
                                    PageStyle.GRID -> {
                                        var y = padTopPx + lhPx
                                        while (y < size.height) {
                                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                                            y += lhPx
                                        }
                                        var x = lhPx
                                        while (x < size.width) {
                                            drawLine(lineColor, Offset(x, padTopPx), Offset(x, size.height), strokeWidth = 0.5f)
                                            x += lhPx
                                        }
                                    }
                                    PageStyle.DOTTED -> {
                                        val dotColor = Color.Black.copy(alpha = (noteLineOpacity * 1.5f).coerceAtMost(1f))
                                        var y = padTopPx + lhPx
                                        while (y < size.height) {
                                            var x = lhPx
                                            while (x < size.width) {
                                                drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                                                x += lhPx
                                            }
                                            y += lhPx
                                        }
                                    }
                                    PageStyle.BLANK -> {}
                                }
                            }
                        }
                )

                // Text field - 15dp horizontal padding, alignment via ParagraphStyle in AnnotatedString
                BasicTextField(
                    value = displayValue,
                    onValueChange = { onContentChange(it) },
                    onTextLayout = { layoutResult -> textLayoutResult = layoutResult },
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        color = Color(0xFF333333),
                        lineHeight = lineHeightSp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Bottom,
                            trim = LineHeightStyle.Trim.FirstLineTop
                        )
                    ),
                    cursorBrush = SolidColor(Color(0xFFD2691E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = screenHeightDp.dp)
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (contentText.isEmpty()) {
                                Text(
                                    "\u041d\u0430\u0447\u043d\u0438\u0442\u0435 \u043f\u0438\u0441\u0430\u0442\u044c...",
                                    style = TextStyle(
                                        fontSize = fontSize.sp,
                                        color = Color(0xFFBBBBBB),
                                        textAlign = composeTextAlign
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    // Dialogs
    if (showPageStyleDialog) {
        PageStyleDialog(currentStyle = pageStyle, onDismiss = { showPageStyleDialog = false }, onStyleSelected = { pageStyle = it; showPageStyleDialog = false })
    }
    if (showPageColorDialog) {
        PageColorDialog(currentTheme = noteTheme, onDismiss = { showPageColorDialog = false }, onThemeSelected = { noteTheme = it; showPageColorDialog = false })
    }
    if (showHeaderColorDialog) {
        EditorHeaderColorDialog(currentColor = headerColor, onDismiss = { showHeaderColorDialog = false }, onColorSelected = { headerColor = it; showHeaderColorDialog = false })
    }
}

@Composable
fun PageStyleDialog(currentStyle: PageStyle, onDismiss: () -> Unit, onStyleSelected: (PageStyle) -> Unit) {
    val styles = listOf(
        PageStyle.BLANK to "\u0427\u0438\u0441\u0442\u044b\u0439 \u043b\u0438\u0441\u0442",
        PageStyle.LINED to "\u0412 \u043b\u0438\u043d\u0435\u0439\u043a\u0443",
        PageStyle.GRID to "\u0412 \u043a\u043b\u0435\u0442\u043a\u0443",
        PageStyle.DOTTED to "\u0412 \u0442\u043e\u0447\u043a\u0443"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u0422\u0438\u043f \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b") },
        text = {
            Column {
                styles.forEach { (style, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = style == currentStyle, onClick = { onStyleSelected(style) })
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
fun PageColorDialog(currentTheme: NoteTheme, onDismiss: () -> Unit, onThemeSelected: (NoteTheme) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u0426\u0432\u0435\u0442 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NoteTheme.entries.forEach { theme ->
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(theme.color)
                            .then(if (theme == currentTheme) Modifier.background(Color.Black.copy(alpha = 0.15f), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onThemeSelected(theme) }) {
                            if (theme == currentTheme) Icon(Icons.Default.Check, null, tint = Color(0xFF555555), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun EditorHeaderColorDialog(currentColor: HeaderColor, onDismiss: () -> Unit, onColorSelected: (HeaderColor) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u0426\u0432\u0435\u0442 \u0448\u0430\u043f\u043a\u0438") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderColor.entries.forEach { color ->
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(if (color == HeaderColor.NONE) Color(0xFFCCCCCC) else color.color),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onColorSelected(color) }) {
                            if (color == HeaderColor.NONE) Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            else if (color == currentColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
