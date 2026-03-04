package com.goodnotepad.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.goodnotepad.data.*
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

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

fun serializeLineAlignments(alignments: Map<Int, TextAlign>): String {
    if (alignments.isEmpty()) return ""
    val obj = JSONObject()
    alignments.forEach { (lineIdx, align) -> obj.put(lineIdx.toString(), align.name) }
    return obj.toString()
}

fun deserializeLineAlignments(json: String): Map<Int, TextAlign> {
    if (json.isBlank()) return emptyMap()
    val result = mutableMapOf<Int, TextAlign>()
    try {
        val obj = JSONObject(json)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val alignName = obj.getString(key)
            result[key.toInt()] = try { TextAlign.valueOf(alignName) } catch (_: Exception) { TextAlign.LEFT }
        }
    } catch (_: Exception) {}
    return result
}

/**
 * VisualTransformation that applies per-character formatting (SpanStyle)
 * AND per-line alignment (ParagraphStyle with textAlign only).
 * ParagraphStyle does NOT set lineHeight (TextStyle handles that to avoid doubling).
 * Grid lines are drawn from TextLayoutResult to match actual text positions.
 */
class FormattingTransformation(
    private val formats: List<CharFormat>,
    private val lineAlignments: Map<Int, TextAlign>,
    private val defaultAlign: TextAlign
) : VisualTransformation {
    private fun mapAlign(align: TextAlign): androidx.compose.ui.text.style.TextAlign = when (align) {
        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
        TextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = buildAnnotatedString {
            append(text)
            // SpanStyle for character formatting (bold, italic, etc.)
            if (formats.isNotEmpty()) {
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
            // ParagraphStyle for per-line alignment (textAlign only, no lineHeight)
            val str = text.text
            if (str.isNotEmpty()) {
                var lineIdx = 0
                var lineStart = 0
                for (pos in str.indices) {
                    if (str[pos] == '\n') {
                        val align = lineAlignments[lineIdx] ?: defaultAlign
                        addStyle(ParagraphStyle(textAlign = mapAlign(align)), lineStart, pos + 1)
                        lineIdx++
                        lineStart = pos + 1
                    }
                }
                // Last line (may not end with \n)
                if (lineStart < str.length) {
                    val align = lineAlignments[lineIdx] ?: defaultAlign
                    addStyle(ParagraphStyle(textAlign = mapAlign(align)), lineStart, str.length)
                }
            }
        }
        return TransformedText(formatted, OffsetMapping.Identity)
    }
}

// Undo/Redo snapshot
data class UndoSnapshot(
    val text: String,
    val selection: TextRange,
    val formats: List<CharFormat>,
    val lineAlignments: Map<Int, TextAlign> = emptyMap()
)

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
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // State
    var titleText by remember { mutableStateOf("") }
    // Single TextFieldValue state — atomic updates prevent IME word duplication
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var noteTheme by remember { mutableStateOf(NoteTheme.WHITE) }
    var pageStyle by remember { mutableStateOf(PageStyle.LINED) }
    var headerColor by remember { mutableStateOf(HeaderColor.NONE) }
    var fontSize by remember { mutableIntStateOf(14) }
    var textAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var titleTextAlign by remember { mutableStateOf(TextAlign.LEFT) }
    var noteLineOpacity by remember { mutableFloatStateOf(0.5f) }
    var isFavorite by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var titleFontColorArgb by remember { mutableIntStateOf(0xFF333333.toInt()) }
    var contentFontColorArgb by remember { mutableIntStateOf(0xFF333333.toInt()) }

    val charFormats = remember { mutableListOf<CharFormat>() }
    val lineAlignments = remember { mutableMapOf<Int, TextAlign>() }
    var formatVersion by remember { mutableIntStateOf(0) }
    var alignVersion by remember { mutableIntStateOf(0) }
    var activeFormat by remember { mutableStateOf(CharFormat()) }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Undo/Redo stacks
    val undoStack = remember { mutableListOf<UndoSnapshot>() }
    val redoStack = remember { mutableListOf<UndoSnapshot>() }
    var undoVersion by remember { mutableIntStateOf(0) }

    fun pushUndo() {
        undoStack.add(UndoSnapshot(contentValue.text, contentValue.selection, charFormats.toList(), lineAlignments.toMap()))
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        undoVersion++
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        redoStack.add(UndoSnapshot(contentValue.text, contentValue.selection, charFormats.toList(), lineAlignments.toMap()))
        val snap = undoStack.removeAt(undoStack.lastIndex)
        contentValue = TextFieldValue(snap.text, snap.selection)
        charFormats.clear()
        charFormats.addAll(snap.formats)
        lineAlignments.clear()
        lineAlignments.putAll(snap.lineAlignments)
        formatVersion++
        alignVersion++
        undoVersion++
    }

    fun performRedo() {
        if (redoStack.isEmpty()) return
        undoStack.add(UndoSnapshot(contentValue.text, contentValue.selection, charFormats.toList(), lineAlignments.toMap()))
        val snap = redoStack.removeAt(redoStack.lastIndex)
        contentValue = TextFieldValue(snap.text, snap.selection)
        charFormats.clear()
        charFormats.addAll(snap.formats)
        lineAlignments.clear()
        lineAlignments.putAll(snap.lineAlignments)
        formatVersion++
        alignVersion++
        undoVersion++
    }

    // Load note: single LaunchedEffect that waits for the correct note from Flow
    LaunchedEffect(noteId) {
        initialized = false
        titleText = ""
        contentValue = TextFieldValue("")
        charFormats.clear()
        lineAlignments.clear()
        undoStack.clear()
        redoStack.clear()
        formatVersion++
        alignVersion++
        undoVersion++
        viewModel.loadNote(noteId)
        // Wait until the correct note arrives in the flow
        val loadedNote = viewModel.currentNote.first { it != null && it.id == noteId }
        if (loadedNote != null && !initialized) {
            titleText = loadedNote.title
            contentValue = TextFieldValue(loadedNote.content, TextRange(loadedNote.content.length))
            charFormats.clear()
            charFormats.addAll(deserializeFormats(loadedNote.formatting, loadedNote.content.length))
            lineAlignments.clear()
            lineAlignments.putAll(deserializeLineAlignments(loadedNote.lineAlignments))
            formatVersion++
            alignVersion++
            noteTheme = loadedNote.theme
            pageStyle = loadedNote.pageStyle
            headerColor = loadedNote.headerColor
            fontSize = loadedNote.fontSize
            textAlign = loadedNote.textAlign
            titleTextAlign = loadedNote.titleTextAlign
            noteLineOpacity = loadedNote.lineOpacity
            isFavorite = loadedNote.isFavorite
            titleFontColorArgb = loadedNote.titleFontColor.toInt()
            contentFontColorArgb = loadedNote.contentFontColor.toInt()
            initialized = true
        }
    }

    // Save note when state changes
    LaunchedEffect(titleText, contentValue.text, textAlign, titleTextAlign, pageStyle, noteTheme, headerColor, fontSize, noteLineOpacity, formatVersion, alignVersion, titleFontColorArgb, contentFontColorArgb) {
        if (initialized) {
            note?.let {
                viewModel.saveNote(it.copy(
                    title = titleText, content = contentValue.text, preview = contentValue.text.take(100),
                    formatting = serializeFormats(charFormats),
                    lineAlignments = serializeLineAlignments(lineAlignments),
                    titleFontColor = titleFontColorArgb,
                    contentFontColor = contentFontColorArgb,
                    theme = noteTheme, pageStyle = pageStyle, headerColor = headerColor,
                    fontSize = fontSize, textAlign = textAlign, titleTextAlign = titleTextAlign,
                    lineOpacity = noteLineOpacity, isFavorite = isFavorite
                ))
            }
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showPageStyleDialog by remember { mutableStateOf(false) }
    var showPageColorDialog by remember { mutableStateOf(false) }
    var showHeaderColorDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showHighlightMenu by remember { mutableStateOf(false) }
    var showTitleFontColorDialog by remember { mutableStateOf(false) }
    var showContentFontColorDialog by remember { mutableStateOf(false) }

    val composeTitleTextAlign = when (titleTextAlign) {
        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
        TextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
    }
    val lineHeightSp = (fontSize * 1.5f).sp

    // Formatting via VisualTransformation (SpanStyle + ParagraphStyle for alignment)
    val formatsSnapshot = remember(formatVersion) { charFormats.toList() }
    val lineAlignSnapshot = remember(alignVersion) { lineAlignments.toMap() }
    val contentVisualTransformation = remember(formatsSnapshot, lineAlignSnapshot, textAlign) {
        FormattingTransformation(formatsSnapshot, lineAlignSnapshot, textAlign)
    }

    // Current cursor line index for UI (alignment icon, button highlight)
    val currentLineIdx = remember(contentValue.selection, contentValue.text) {
        val cp = contentValue.selection.start.coerceIn(0, contentValue.text.length)
        contentValue.text.substring(0, cp).count { it == '\n' }
    }
    val effectiveTextAlign = lineAlignments[currentLineIdx] ?: textAlign
    val alignIcon = when (effectiveTextAlign) {
        TextAlign.LEFT -> Icons.Default.FormatAlignLeft
        TextAlign.CENTER -> Icons.Default.FormatAlignCenter
        TextAlign.RIGHT -> Icons.Default.FormatAlignRight
        TextAlign.JUSTIFY -> Icons.Default.FormatAlignJustify
    }
    // Track programmatic text changes
    var isInternalChange by remember { mutableStateOf(false) }

    fun onContentChange(newValue: TextFieldValue) {
        if (isInternalChange) return
        val oldText = contentValue.text
        val newText = newValue.text
        if (newText != oldText) {
            pushUndo()
            if (newText.length > oldText.length) {
                val insertLen = newText.length - oldText.length
                val insertPos = (newValue.selection.start - insertLen).coerceIn(0, charFormats.size)
                repeat(insertLen) { charFormats.add(insertPos, activeFormat.copy()) }
            } else if (newText.length < oldText.length) {
                val deleteLen = oldText.length - newText.length
                val deletePos = newValue.selection.start.coerceIn(0, charFormats.size)
                repeat(deleteLen) { if (deletePos < charFormats.size) charFormats.removeAt(deletePos) }
            }
            formatVersion++
        }
        contentValue = newValue  // Single atomic update — text + selection + composition
    }

    fun toggleFmt(getter: (CharFormat) -> Boolean, setter: (CharFormat, Boolean) -> CharFormat) {
        val sel = contentValue.selection
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
        val sel = contentValue.selection
        if (!sel.collapsed && sel.min < charFormats.size) {
            val end = sel.max.coerceAtMost(charFormats.size)
            for (i in sel.min until end) { charFormats[i] = charFormats[i].copy(highlightColor = color) }
            formatVersion++
        }
    }

    val isBoldActive = run {
        val sel = contentValue.selection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.bold == true }
        else activeFormat.bold
    }
    val isItalicActive = run {
        val sel = contentValue.selection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.italic == true }
        else activeFormat.italic
    }
    val isUnderlineActive = run {
        val sel = contentValue.selection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.underline == true }
        else activeFormat.underline
    }
    val isStrikethroughActive = run {
        val sel = contentValue.selection
        if (!sel.collapsed && sel.min < charFormats.size) (sel.min until sel.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.strikethrough == true }
        else activeFormat.strikethrough
    }

    // Dynamic separator: measure actual text area width using TextMeasurer for accuracy
    val density = LocalDensity.current
    var contentAreaWidthPx by remember { mutableIntStateOf(0) }
    val textMeasurer = rememberTextMeasurer()
    val separatorCharCount = remember(fontSize, contentAreaWidthPx) {
        if (contentAreaWidthPx <= 0) return@remember 40
        val charWidth = textMeasurer.measure("\u2500", TextStyle(fontSize = fontSize.sp)).size.width
        if (charWidth > 0) (contentAreaWidthPx / charWidth).coerceIn(10, 300) else 40
    }

    val titleFontColor = Color(titleFontColorArgb)
    val contentFontColor = Color(contentFontColorArgb)

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
                        // Undo button
                        IconButton(onClick = { performUndo() }, enabled = undoStack.isNotEmpty(), modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Undo, contentDescription = "\u041e\u0442\u043c\u0435\u043d\u0430", tint = if (undoStack.isNotEmpty()) AppTitle else Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
                        }
                        // Redo button
                        IconButton(onClick = { performRedo() }, enabled = redoStack.isNotEmpty(), modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Redo, contentDescription = "\u0412\u043e\u0437\u0432\u0440\u0430\u0442", tint = if (redoStack.isNotEmpty()) AppTitle else Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showSearch = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "\u041f\u043e\u0438\u0441\u043a", tint = AppTitle, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            IconButton(onClick = { showAlignMenu = true }, modifier = Modifier.size(40.dp)) {
                                Icon(alignIcon, contentDescription = "\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435", tint = AppTitle, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showAlignMenu, onDismissRequest = { showAlignMenu = false }) {
                                Text("\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435 \u0448\u0430\u043f\u043a\u0438", fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = { titleTextAlign = TextAlign.LEFT; showAlignMenu = false }) { Icon(Icons.Default.FormatAlignLeft, null, tint = if (titleTextAlign == TextAlign.LEFT) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { titleTextAlign = TextAlign.CENTER; showAlignMenu = false }) { Icon(Icons.Default.FormatAlignCenter, null, tint = if (titleTextAlign == TextAlign.CENTER) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { titleTextAlign = TextAlign.RIGHT; showAlignMenu = false }) { Icon(Icons.Default.FormatAlignRight, null, tint = if (titleTextAlign == TextAlign.RIGHT) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { titleTextAlign = TextAlign.JUSTIFY; showAlignMenu = false }) { Icon(Icons.Default.FormatAlignJustify, null, tint = if (titleTextAlign == TextAlign.JUSTIFY) Color(0xFFD2691E) else Color(0xFF666666)) }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("\u0412\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u043a\u0441\u0442\u0430", fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    // Per-line alignment: sets alignment for current line + updates global textAlign for rendering
                                    fun setLineAlign(align: TextAlign) {
                                        val startIdx = contentValue.text.substring(0, contentValue.selection.start.coerceIn(0, contentValue.text.length)).count { it == '\n' }
                                        val endIdx = contentValue.text.substring(0, contentValue.selection.end.coerceIn(0, contentValue.text.length)).count { it == '\n' }
                                        for (li in startIdx..endIdx) { lineAlignments[li] = align }
                                        textAlign = align
                                        alignVersion++
                                        showAlignMenu = false
                                    }
                                    IconButton(onClick = { setLineAlign(TextAlign.LEFT) }) { Icon(Icons.Default.FormatAlignLeft, null, tint = if (effectiveTextAlign == TextAlign.LEFT) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { setLineAlign(TextAlign.CENTER) }) { Icon(Icons.Default.FormatAlignCenter, null, tint = if (effectiveTextAlign == TextAlign.CENTER) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { setLineAlign(TextAlign.RIGHT) }) { Icon(Icons.Default.FormatAlignRight, null, tint = if (effectiveTextAlign == TextAlign.RIGHT) Color(0xFFD2691E) else Color(0xFF666666)) }
                                    IconButton(onClick = { setLineAlign(TextAlign.JUSTIFY) }) { Icon(Icons.Default.FormatAlignJustify, null, tint = if (effectiveTextAlign == TextAlign.JUSTIFY) Color(0xFFD2691E) else Color(0xFF666666)) }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        IconButton(onClick = {
                            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "$titleText\n\n${contentValue.text}"); type = "text/plain" }
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
                                DropdownMenuItem(text = { Text("\u0426\u0432\u0435\u0442 \u0448\u0440\u0438\u0444\u0442\u0430 \u0448\u0430\u043f\u043a\u0438") }, onClick = { showTitleFontColorDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.FormatColorText, null) })
                                DropdownMenuItem(text = { Text("\u0426\u0432\u0435\u0442 \u0448\u0440\u0438\u0444\u0442\u0430 \u0442\u0435\u043a\u0441\u0442\u0430") }, onClick = { showContentFontColorDialog = true; showMoreMenu = false }, leadingIcon = { Icon(Icons.Default.FormatColorText, null) })
                                HorizontalDivider()
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("\u0420\u0430\u0437\u043c\u0435\u0440 \u0448\u0440\u0438\u0444\u0442\u0430: ${fontSize}sp", fontSize = 13.sp, color = Color(0xFF555555))
                                    Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.roundToInt() }, valueRange = 10f..30f, steps = 19, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)))
                                }
                                HorizontalDivider()
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u043b\u0438\u043d\u0435\u0435\u043a: ${(noteLineOpacity * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF555555))
                                    Slider(value = noteLineOpacity, onValueChange = { noteLineOpacity = it }, valueRange = 0.05f..1f, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color(0xFFD2691E), activeTrackColor = Color(0xFFD2691E)))
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
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
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
                        pushUndo()
                        val ct = contentValue.text
                        val nl = if (ct.endsWith("\n") || ct.isEmpty()) "" else "\n"
                        val ins = nl + "\u2022 "
                        val pos = contentValue.selection.start.coerceIn(0, ct.length)
                        isInternalChange = true
                        val newT = ct.substring(0, pos) + ins + ct.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentValue = TextFieldValue(newT, TextRange(pos + ins.length))
                        formatVersion++
                        isInternalChange = false
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "\u0421\u043f\u0438\u0441\u043e\u043a", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        pushUndo()
                        val ct2 = contentValue.text
                        val pos = contentValue.selection.start.coerceIn(0, ct2.length)
                        val textBefore = ct2.substring(0, pos)
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
                        val nl = if (textBefore.endsWith("\n") || ct2.isEmpty()) "" else "\n"
                        val ins = nl + "${lastNum + 1}. "
                        isInternalChange = true
                        val newT2 = ct2.substring(0, pos) + ins + ct2.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentValue = TextFieldValue(newT2, TextRange(pos + ins.length))
                        formatVersion++
                        isInternalChange = false
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "\u041d\u0443\u043c\u0435\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0439", tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        pushUndo()
                        val ct3 = contentValue.text
                        val nl = if (ct3.endsWith("\n") || ct3.isEmpty()) "" else "\n"
                        val ins = nl + "\u2500".repeat(separatorCharCount) + "\n"
                        val pos = contentValue.selection.start.coerceIn(0, ct3.length)
                        isInternalChange = true
                        val newT3 = ct3.substring(0, pos) + ins + ct3.substring(pos)
                        repeat(ins.length) { charFormats.add(pos.coerceIn(0, charFormats.size), CharFormat()) }
                        contentValue = TextFieldValue(newT3, TextRange(pos + ins.length))
                        formatVersion++
                        isInternalChange = false
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
            // === TITLE ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (headerColor != HeaderColor.NONE) headerColor.color else Color.Transparent)
                    .padding(horizontal = 15.dp, vertical = 12.dp)
            ) {
                if (titleText.isEmpty()) {
                    Text(
                        "\u0417\u0430\u0433\u043e\u043b\u043e\u0432\u043e\u043a",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFFBBBBBB),
                            textAlign = composeTitleTextAlign
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = titleFontColor,
                        textAlign = composeTitleTextAlign
                    ),
                    cursorBrush = SolidColor(Color(0xFFD2691E)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 15.dp), color = Color(0x20000000))

            // === CONTENT ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeightDp.dp)
            ) {
                // Adaptive grid: draws lines at actual text line positions from TextLayoutResult.
                // This ensures grid always matches text regardless of ParagraphStyle spacing.
                // Falls back to fixed intervals below the text or when layout is unavailable.
                val lineColor = Color.Black.copy(alpha = noteLineOpacity)
                val currentPageStyle = pageStyle
                val lhSp = lineHeightSp
                val currentLayout = textLayoutResult

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val lhPx = lhSp.toPx()
                            val padTopPx = 8.dp.toPx()

                            // Collect Y positions for horizontal grid lines
                            val yPositions = mutableListOf<Float>()
                            if (currentLayout != null && currentLayout.lineCount > 0) {
                                // Use actual text line bottom positions (offset by padding)
                                for (li in 0 until currentLayout.lineCount) {
                                    yPositions.add(padTopPx + currentLayout.getLineBottom(li))
                                }
                                // Continue with fixed intervals below last text line
                                var y = yPositions.last() + lhPx
                                while (y < size.height) {
                                    yPositions.add(y)
                                    y += lhPx
                                }
                            } else {
                                // Fallback: fixed intervals when no layout available
                                var y = padTopPx + lhPx
                                while (y < size.height) {
                                    yPositions.add(y)
                                    y += lhPx
                                }
                            }

                            when (currentPageStyle) {
                                PageStyle.LINED -> {
                                    for (y in yPositions) {
                                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                                    }
                                }
                                PageStyle.GRID -> {
                                    for (y in yPositions) {
                                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                                    }
                                    var x = lhPx
                                    while (x < size.width) {
                                        drawLine(lineColor, Offset(x, padTopPx), Offset(x, size.height), strokeWidth = 0.5f)
                                        x += lhPx
                                    }
                                }
                                PageStyle.DOTTED -> {
                                    val dotColor = Color.Black.copy(alpha = (noteLineOpacity * 1.5f).coerceAtMost(1f))
                                    for (y in yPositions) {
                                        var x = lhPx
                                        while (x < size.width) {
                                            drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                                            x += lhPx
                                        }
                                    }
                                }
                                PageStyle.BLANK -> {}
                            }
                        }
                )

                // Text input placeholder
                if (contentValue.text.isEmpty()) {
                    Text(
                        "\u041d\u0430\u0447\u043d\u0438\u0442\u0435 \u043f\u0438\u0441\u0430\u0442\u044c...",
                        style = TextStyle(
                            fontSize = fontSize.sp,
                            color = Color(0xFFBBBBBB),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 8.dp)
                    )
                }
                BasicTextField(
                    value = contentValue,
                    onValueChange = { onContentChange(it) },
                    onTextLayout = { layoutResult -> textLayoutResult = layoutResult },
                    visualTransformation = contentVisualTransformation,
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        color = contentFontColor,
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
                        .padding(horizontal = 15.dp, vertical = 8.dp)
                        .onSizeChanged { size -> contentAreaWidthPx = size.width }
                )
            }
        }
    }

    if (showPageStyleDialog) {
        PageStyleDialog(currentStyle = pageStyle, onDismiss = { showPageStyleDialog = false }, onStyleSelected = { pageStyle = it; showPageStyleDialog = false })
    }
    if (showPageColorDialog) {
        PageColorDialog(currentTheme = noteTheme, onDismiss = { showPageColorDialog = false }, onThemeSelected = { noteTheme = it; showPageColorDialog = false })
    }
    if (showHeaderColorDialog) {
        EditorHeaderColorDialog(currentColor = headerColor, onDismiss = { showHeaderColorDialog = false }, onColorSelected = { headerColor = it; showHeaderColorDialog = false })
    }
    if (showTitleFontColorDialog) {
        FontColorDialog(
            title = "\u0426\u0432\u0435\u0442 \u0448\u0440\u0438\u0444\u0442\u0430 \u0448\u0430\u043f\u043a\u0438",
            currentColorArgb = titleFontColorArgb,
            onDismiss = { showTitleFontColorDialog = false },
            onColorSelected = { titleFontColorArgb = it; showTitleFontColorDialog = false }
        )
    }
    if (showContentFontColorDialog) {
        FontColorDialog(
            title = "\u0426\u0432\u0435\u0442 \u0448\u0440\u0438\u0444\u0442\u0430 \u0442\u0435\u043a\u0441\u0442\u0430",
            currentColorArgb = contentFontColorArgb,
            onDismiss = { showContentFontColorDialog = false },
            onColorSelected = { contentFontColorArgb = it; showContentFontColorDialog = false }
        )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFCCCCCC)), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { onColorSelected(HeaderColor.NONE) }) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("\u0411\u0435\u0437 \u0446\u0432\u0435\u0442\u0430", fontSize = 14.sp, color = Color(0xFF666666))
                }
                Text("\u041f\u0430\u0441\u0442\u0435\u043b\u044c\u043d\u044b\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("PASTEL") }.forEach { color ->
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { onColorSelected(color) }) {
                                if (color == currentColor) Icon(Icons.Default.Check, null, tint = Color(0xFF555555), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Text("\u042f\u0440\u043a\u0438\u0435", fontSize = 12.sp, color = Color(0xFF888888))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    HeaderColor.entries.filter { it.name.startsWith("VIBRANT") }.forEach { color ->
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.color), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { onColorSelected(color) }) {
                                if (color == currentColor) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
fun FontColorDialog(title: String, currentColorArgb: Int, onDismiss: () -> Unit, onColorSelected: (Int) -> Unit) {
    data class ColorOption(val argb: Int, val label: String)
    val colors = listOf(
        ColorOption(0xFF333333.toInt(), "\u0427\u0451\u0440\u043d\u044b\u0439"),
        ColorOption(0xFF555555.toInt(), "\u0422\u0451\u043c\u043d\u043e-\u0441\u0435\u0440\u044b\u0439"),
        ColorOption(0xFF888888.toInt(), "\u0421\u0435\u0440\u044b\u0439"),
        ColorOption(0xFF5D4037.toInt(), "\u041a\u043e\u0440\u0438\u0447\u043d\u0435\u0432\u044b\u0439"),
        ColorOption(0xFFD32F2F.toInt(), "\u041a\u0440\u0430\u0441\u043d\u044b\u0439"),
        ColorOption(0xFF1976D2.toInt(), "\u0421\u0438\u043d\u0438\u0439"),
        ColorOption(0xFF388E3C.toInt(), "\u0417\u0435\u043b\u0451\u043d\u044b\u0439"),
        ColorOption(0xFF7B1FA2.toInt(), "\u0424\u0438\u043e\u043b\u0435\u0442\u043e\u0432\u044b\u0439"),
        ColorOption(0xFFE65100.toInt(), "\u041e\u0440\u0430\u043d\u0436\u0435\u0432\u044b\u0439"),
        ColorOption(0xFF00695C.toInt(), "\u0411\u0438\u0440\u044e\u0437\u043e\u0432\u044b\u0439"),
        ColorOption(0xFFFFFFFF.toInt(), "\u0411\u0435\u043b\u044b\u0439")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                colors.forEach { opt ->
                    val c = Color(opt.argb)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (opt.argb == 0xFFFFFFFF.toInt())
                                    Modifier.border(1.dp, Color(0xFFCCCCCC), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onColorSelected(opt.argb) }) {
                            if (opt.argb == currentColorArgb) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = if (opt.argb == 0xFFFFFFFF.toInt()) Color(0xFF333333) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
