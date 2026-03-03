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
    modifier: Modifier = Modifier
) {
    val lineSpacing = (fontSize + 8).toFloat()
    val lineColor = Color(0x20000000)
    val dotColor = Color(0x30000000)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Fill background
        drawRect(color = noteTheme.color)

        when (pageStyle) {
            PageStyle.LINED -> {
                var y = lineSpacing * 2 // Start after some top padding
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
                // Horizontal lines
                var y = gridSize
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5f
                    )
                    y += gridSize
                }
                // Vertical lines
                var x = gridSize
                while (x < size.width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 0.5f
                    )
                    x += gridSize
                }
            }
            PageStyle.DOTTED -> {
                val dotSpacing = lineSpacing
                var y = dotSpacing
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
