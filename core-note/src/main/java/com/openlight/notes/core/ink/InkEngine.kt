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

/** Undoable command (AD-2, Phase 7 reuse). */
sealed class InkCommand {
    abstract fun apply()
    abstract fun undo()

    class AddStroke(val stroke: Stroke, val blockId: String, val data: MutableList<Stroke>) : InkCommand() {
        override fun apply() { data.add(stroke) }
        override fun undo() { data.remove(stroke) }
    }

    class RemoveStroke(val stroke: Stroke, val blockId: String, val data: MutableList<Stroke>) : InkCommand() {
        override fun apply() { data.remove(stroke) }
        override fun undo() { data.add(stroke) }
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
