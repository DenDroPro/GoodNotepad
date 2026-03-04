package com.goodnotepad.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodnotepad.data.HeaderColor
import com.goodnotepad.data.Note
import com.goodnotepad.data.ViewMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewMode: ViewMode = ViewMode.GRID_2,
    modifier: Modifier = Modifier
) {
    val titleFontSize = when (viewMode) {
        ViewMode.GRID_5 -> 9.sp
        ViewMode.GRID_4 -> 10.sp
        ViewMode.GRID_3 -> 11.sp
        else -> 12.sp
    }
    val previewFontSize = when (viewMode) {
        ViewMode.GRID_5 -> 6.sp
        ViewMode.GRID_4 -> 7.sp
        ViewMode.GRID_3 -> 8.sp
        else -> 9.sp
    }
    val dateFontSize = when (viewMode) {
        ViewMode.GRID_5 -> 6.sp
        ViewMode.GRID_4 -> 7.sp
        ViewMode.GRID_3 -> 8.sp
        else -> 9.sp
    }
    val previewMaxLines = when (viewMode) {
        ViewMode.GRID_5 -> 8
        ViewMode.GRID_4 -> 10
        ViewMode.GRID_3 -> 12
        else -> 16
    }
    val iconSize = when (viewMode) {
        ViewMode.GRID_5 -> 8.dp
        ViewMode.GRID_4 -> 10.dp
        else -> 12.dp
    }
    val hPad = when (viewMode) {
        ViewMode.GRID_5 -> 4.dp
        ViewMode.GRID_4 -> 5.dp
        else -> 8.dp
    }
    val vPad = when (viewMode) {
        ViewMode.GRID_5 -> 3.dp
        ViewMode.GRID_4 -> 4.dp
        else -> 6.dp
    }

    Card(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = note.theme.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (note.headerColor != HeaderColor.NONE) note.headerColor.color
                        else note.theme.color
                    )
                    .padding(horizontal = hPad, vertical = vPad),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title.ifBlank { "\u0411\u0435\u0437 \u0437\u0430\u0433\u043e\u043b\u043e\u0432\u043a\u0430" },
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(Icons.Filled.PushPin, null, Modifier.size(iconSize), tint = Color(0xFF8B6914))
                    }
                    if (note.isFavorite) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Filled.Star, null, Modifier.size(iconSize), tint = Color(0xFFFFA000))
                    }
                }
            }
            HorizontalDivider(color = Color(0x15000000), thickness = 0.5.dp)
            // Show full content as preview, scaled down to fit
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = hPad, vertical = 2.dp)
            ) {
                val previewText = note.content.ifBlank { note.preview }
                if (previewText.isNotBlank()) {
                    Text(
                        text = previewText,
                        fontSize = previewFontSize,
                        maxLines = previewMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF555555),
                        lineHeight = previewFontSize * 1.2f
                    )
                }
            }
            Text(
                text = formatDate(note.updatedAt),
                fontSize = dateFontSize,
                color = Color(0xFFAAAAAA),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = hPad, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = note.theme.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (note.headerColor != HeaderColor.NONE) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(note.headerColor.color))
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(note.title.ifBlank { "\u0411\u0435\u0437 \u0437\u0430\u0433\u043e\u043b\u043e\u0432\u043a\u0430" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF333333))
                Text(note.preview.ifBlank { "\u041f\u0443\u0441\u0442\u0430\u044f \u0437\u0430\u043c\u0435\u0442\u043a\u0430" }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF888888))
                Text(formatDate(note.updatedAt), fontSize = 11.sp, color = Color(0xFFAAAAAA))
            }
            Column(
                modifier = Modifier.padding(8.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (note.isPinned) {
                    Icon(Icons.Filled.PushPin, null, Modifier.size(16.dp), tint = Color(0xFF8B6914))
                }
                if (note.isFavorite) {
                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFA000))
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
