package com.goodnotepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.goodnotepad.data.NoteTheme
import com.goodnotepad.data.PageStyle

// Issue #5: Line height depends on font size + a few pixels
// Issue #13: Header area (title) has no lines/grid/dots - clean background
@Composable
fun PageBackground(
    pageStyle: PageStyle,
    noteTheme: NoteTheme,
    fontSize: Int,
    headerHeightPx: Float = 0f,
    modifier: Modifier = Modifier
) {
    // lineSpacing = fontSize + 4dp equivalent (tight to font)
    val lineSpacing = (fontSize + 4).toFloat() * 2.5f
    val lineColor = Color(0x20000000)
    val dotColor = Color(0x30000000)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = noteTheme.color)

        // Start drawing lines/grid/dots BELOW the header area
        val startY = headerHeightPx + lineSpacing

        when (pageStyle) {
            PageStyle.LINED -> {
                var y = startY
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += lineSpacing
                }
            }
            PageStyle.GRID -> {
                val gridSize = lineSpacing
                var y = startY
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5f
                    )
                    y += gridSize
                }
                var x = gridSize
                while (x < size.width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, startY),
                        end = Offset(x, size.height),
                        strokeWidth = 0.5f
                    )
                    x += gridSize
                }
            }
            PageStyle.DOTTED -> {
                val dotSpacing = lineSpacing
                var y = startY
                while (y < size.height) {
                    var x = dotSpacing
                    while (x < size.width) {
                        drawCircle(
                            color = dotColor,
                            radius = 1.5f,
                            center = Offset(x, y)
                        )
                        x += dotSpacing
                    }
                    y += dotSpacing
                }
            }
            PageStyle.BLANK -> {
                // Just the background color, already drawn
            }
        }
    }
}
