package com.openlight.notes.ui.ink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openlight.notes.core.ink.Brush
import com.openlight.notes.core.ink.BrushType

/**
 * Ink toolbar: brush selection, color, size, undo/redo, eraser toggle.
 */
@Composable
fun InkToolbar(
    currentBrush: Brush,
    isEraser: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onBrushSelected: (Brush) -> Unit,
    onColorSelected: (String) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onEraserToggled: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Top row: back, undo, redo, eraser
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Row {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // Eraser toggle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isEraser) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onEraserToggled() },
                contentAlignment = Alignment.Center
            ) {
                Text("E", color = if (isEraser) Color.White else Color.Black)
            }
        }

        // Brush selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BrushType.values().filter { it != BrushType.ERASER }.forEach { type ->
                val brush = when (type) {
                    BrushType.PEN -> Brush("pen", currentBrush.color, 3.5f)
                    BrushType.MARKER -> Brush("marker", currentBrush.color, 8f)
                    BrushType.HIGHLIGHTER -> Brush("highlighter", currentBrush.color, 15f)
                    else -> Brush()
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentBrush.type == brush.type && !isEraser)
                                MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable {
                            onBrushSelected(brush)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (type) {
                            BrushType.PEN -> "P"
                            BrushType.MARKER -> "M"
                            BrushType.HIGHLIGHTER -> "H"
                            else -> "?"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Color selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val colors = listOf("#1A1A1A", "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA")
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color)))
                        .border(
                            width = if (currentBrush.color == color) 3.dp else 1.dp,
                            color = if (currentBrush.color == color) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }

        // Size slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = currentBrush.size,
                onValueChange = { onSizeChanged(it) },
                valueRange = 1f..20f,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
