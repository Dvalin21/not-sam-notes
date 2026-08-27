package com.openlight.notes.core.ink

import kotlinx.serialization.Serializable

/** Brush types (AD-3). */
enum class BrushType { PEN, MARKER, HIGHLIGHTER, ERASER }

/** Brush definition. */
@Serializable
data class Brush(
    val type: String = "pen",
    val color: String = "#1A1A1A",
    val size: Float = 3.5f
)

/** A single stroke: brush + points. */
@Serializable
data class Stroke(
    val brush: Brush = Brush(),
    val points: List<FloatArray> = emptyList() // [x, y, tMs, pressure, tiltX, tiltY]
)

/** Stroke data for one ink block (AD-5). */
@Serializable
data class StrokeData(
    val strokes: List<Stroke> = emptyList()
)

/** Undoable command (AD-2, Phase 7 reuse). */
sealed class InkCommand {
    abstract fun apply()
    abstract fun undo()

    class AddStroke(val stroke: Stroke, val blockId: String, val data: StrokeData) : InkCommand() {
        override fun apply() { data.strokes.add(stroke) }
        override fun undo() { data.strokes.remove(stroke) }
    }

    class RemoveStroke(val stroke: Stroke, val blockId: String, val data: StrokeData) : InkCommand() {
        override fun apply() { data.strokes.remove(stroke) }
        override fun undo() { data.strokes.add(stroke) }
    }

    class TransformStrokes(val strokes: List<Stroke>, val dx: Float, val dy: Float) : InkCommand() {
        override fun apply() {
            strokes.forEach { stroke ->
                stroke.points.forEach { point ->
                    point[0] += dx
                    point[1] += dy
                }
            }
        }
        override fun undo() {
            strokes.forEach { stroke ->
                stroke.points.forEach { point ->
                    point[0] -= dx
                    point[1] -= dy
                }
            }
        }
    }
}

/** Bounded undo/redo stack. */
class UndoStack(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<InkCommand>()
    private val redoStack = ArrayDeque<InkCommand>()

    fun execute(command: InkCommand) {
        command.apply()
        undoStack.addLast(command)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val command = undoStack.removeLast()
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val command = redoStack.removeLast()
        command.apply()
        undoStack.addLast(command)
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
