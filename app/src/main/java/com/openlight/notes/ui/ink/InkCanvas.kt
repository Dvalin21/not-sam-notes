package com.openlight.notes.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.openlight.notes.core.ink.Brush
import com.openlight.notes.core.ink.Stroke as InkStroke

/**
 * Phase 2: Ink canvas using raw pointer input (Phase 2a).
 * Will be upgraded to androidx.ink in Phase 2b after device input probing.
 * 
 * Now supports onCanvasTap for placing text/image blocks in canvas mode.
 */
@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    currentBrush: Brush,
    onStrokeFinished: (InkStroke) -> Unit,
    onErase: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    isEraser: Boolean = false,
    pageTemplate: String = "blank",
    onCanvasTap: ((Offset) -> Unit)? = null
) {
    val currentPoints = remember { mutableStateListOf<FloatArray>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraser, onCanvasTap) {
                    // If onCanvasTap is provided and we're in a non-drawing mode (text/image placement),
                    // detect taps instead of drags
                    if (onCanvasTap != null && isEraser) {
                        detectTapGestures { offset ->
                            onCanvasTap.invoke(offset)
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (!isEraser) {
                                    currentPoints.clear()
                                    currentPoints.add(
                                        floatArrayOf(
                                            offset.x, offset.y,
                                            System.currentTimeMillis().toFloat(),
                                            0.5f, 0f, 0f
                                        )
                                    )
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (isEraser) {
                                    onErase(change.position)
                                } else {
                                    currentPoints.add(
                                        floatArrayOf(
                                            change.position.x, change.position.y,
                                            System.currentTimeMillis().toFloat(),
                                            0.5f + (change.position.y / size.height).coerceIn(-0.5f, 0.5f),
                                            0f, 0f
                                        )
                                    )
                                }
                            },
                            onDragEnd = {
                                if (!isEraser && currentPoints.isNotEmpty()) {
                                    val stroke = InkStroke(
                                        brush = currentBrush,
                                        points = currentPoints.toList()
                                    )
                                    onStrokeFinished(stroke)
                                    currentPoints.clear()
                                }
                            }
                        )
                    }
                }
                // Separate tap detection for text/image placement when not erasing
                .then(
                    if (onCanvasTap != null && !isEraser) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset -> onCanvasTap.invoke(offset) }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Draw page template
            drawPageTemplate(pageTemplate, size.width, size.height)
            
            // Draw completed strokes
            strokes.forEach { stroke ->
                drawStroke(stroke)
            }

            // Draw current stroke in progress
            if (currentPoints.isNotEmpty()) {
                drawInProgressPoints(currentPoints.toList(), currentBrush)
            }
        }
    }
}

private fun DrawScope.drawPageTemplate(template: String, width: Float, height: Float) {
    val lineColor = Color(0xFFE0E0E0)
    val strokeWidth = 1f
    
    when (template) {
        "lines" -> {
            val lineSpacing = 40f
            var y = lineSpacing
            while (y < height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = strokeWidth
                )
                y += lineSpacing
            }
        }
        "grid" -> {
            val gridSpacing = 40f
            var x = gridSpacing
            while (x < width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = strokeWidth
                )
                x += gridSpacing
            }
            var y = gridSpacing
            while (y < height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = strokeWidth
                )
                y += gridSpacing
            }
        }
        "dots" -> {
            val dotSpacing = 40f
            val dotRadius = 2f
            var y = dotSpacing
            while (y < height) {
                var x = dotSpacing
                while (x < width) {
                    drawCircle(
                        color = lineColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    x += dotSpacing
                }
                y += dotSpacing
            }
        }
    }
}

private fun DrawScope.drawStroke(stroke: InkStroke) {
    val path = Path()
    val points = stroke.points

    if (points.isEmpty()) return

    path.moveTo(points[0][0], points[0][1])
    for (i in 1 until points.size) {
        path.lineTo(points[i][0], points[i][1])
    }

    val brush = stroke.brush
    val color = try {
        Color(android.graphics.Color.parseColor(brush.color))
    } catch (e: Exception) {
        Color.Black
    }

    val width = when (brush.type) {
        "marker" -> brush.size * 3f
        "highlighter" -> brush.size * 5f
        else -> brush.size
    }

    val alpha = when (brush.type) {
        "highlighter" -> 0.4f
        else -> 1f
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawInProgressPoints(points: List<FloatArray>, brush: Brush) {
    if (points.isEmpty()) return

    val path = Path()
    path.moveTo(points[0][0], points[0][1])
    for (i in 1 until points.size) {
        path.lineTo(points[i][0], points[i][1])
    }

    val color = try {
        Color(android.graphics.Color.parseColor(brush.color))
    } catch (e: Exception) {
        Color.Black
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = brush.size,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}