package com.goodnotepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.goodnotepad.data.NoteTheme
import com.goodnotepad.data.PageStyle

@Composable
fun PageBackground(
    pageStyle: PageStyle,
    noteTheme: NoteTheme,
    fontSize: Int,
    headerHeightPx: Float = 0f,
    contentTopOffsetPx: Float = 0f,
    lineOpacity: Float = 0.15f,
    modifier: Modifier = Modifier
) {
    val lineColor = Color.Black.copy(alpha = lineOpacity)
    val dotColor = Color.Black.copy(alpha = (lineOpacity * 1.5f).coerceAtMost(1f))

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = noteTheme.color)

        val densityVal = density
        val lineHeightPx = (fontSize + 4).toFloat() * densityVal

        val startY = if (contentTopOffsetPx > 0f) contentTopOffsetPx + lineHeightPx
                     else headerHeightPx + lineHeightPx

        when (pageStyle) {
            PageStyle.LINED -> {
                var y = startY
                while (y < size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += lineHeightPx
                }
            }
            PageStyle.GRID -> {
                var y = startY
                while (y < size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                    y += lineHeightPx
                }
                var x = lineHeightPx
                while (x < size.width) {
                    drawLine(lineColor, Offset(x, startY), Offset(x, size.height), strokeWidth = 0.5f)
                    x += lineHeightPx
                }
            }
            PageStyle.DOTTED -> {
                var y = startY
                while (y < size.height) {
                    var x = lineHeightPx
                    while (x < size.width) {
                        drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                        x += lineHeightPx
                    }
                    y += lineHeightPx
                }
            }
            PageStyle.BLANK -> { }
        }
    }
}
