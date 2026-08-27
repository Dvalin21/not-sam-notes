package com.openlight.notes.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
 * Phase 9: Partial-stroke eraser.
 * Splits strokes at erase points instead of removing whole strokes.
 */
@Composable
fun InkCanvasWithPartialErase(
    strokes: List<InkStroke>,
    currentBrush: Brush,
    onStrokeFinished: (InkStroke) -> Unit,
    onErase: (Offset) -> Unit,
    onPartialErase: (InkStroke, List<List<FloatArray>>) -> Unit,
    modifier: Modifier = Modifier,
    isEraser: Boolean = false,
    isPartialErase: Boolean = false
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
                .pointerInput(isEraser, isPartialErase) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!isEraser && !isPartialErase) {
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
                            when {
                                isPartialErase -> {
                                    onErase(change.position)
                                }
                                isEraser -> {
                                    onErase(change.position)
                                }
                                else -> {
                                    currentPoints.add(
                                        floatArrayOf(
                                            change.position.x, change.position.y,
                                            System.currentTimeMillis().toFloat(),
                                            0.5f + (change.position.y / size.height).coerceIn(-0.5f, 0.5f),
                                            0f, 0f
                                        )
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            if (!isEraser && !isPartialErase && currentPoints.isNotEmpty()) {
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
        ) {
            strokes.forEach { stroke ->
                drawStroke(stroke)
            }

            if (currentPoints.isNotEmpty()) {
                drawInProgressPoints(currentPoints.toList(), currentBrush)
            }
        }
    }
}

/**
 * Split a stroke at points near the erase position.
 * Returns list of remaining point segments.
 */
fun splitStrokeAtErasePoints(
    stroke: InkStroke,
    erasePoints: List<Offset>,
    radius: Float = 25f
): List<List<FloatArray>> {
    if (stroke.points.isEmpty()) return emptyList()

    val remainingSegments = mutableListOf<MutableList<FloatArray>>()
    var currentSegment = mutableListOf<FloatArray>()

    for (point in stroke.points) {
        val isErased = erasePoints.any { erasePoint ->
            val dx = point[0] - erasePoint.x
            val dy = point[1] - erasePoint.y
            dx * dx + dy * dy < radius * radius
        }

        if (isErased) {
            if (currentSegment.isNotEmpty()) {
                remainingSegments.add(currentSegment)
                currentSegment = mutableListOf()
            }
        } else {
            currentSegment.add(point)
        }
    }

    if (currentSegment.isNotEmpty()) {
        remainingSegments.add(currentSegment)
    }

    return remainingSegments
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
