package com.openlight.notes.ui.ink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openlight.notes.core.ink.Brush
import com.openlight.notes.core.ink.Stroke
import com.openlight.notes.core.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InkUiState(
    val strokes: List<Stroke> = emptyList(),
    val currentBrush: Brush = Brush(),
    val isEraser: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class InkViewModel(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModel() {
    private val _state = MutableStateFlow(InkUiState())
    val state: StateFlow<InkUiState> = _state.asStateFlow()

    private val strokes = mutableListOf<Stroke>()
    private val undoStack = ArrayDeque<List<Stroke>>()
    private val redoStack = ArrayDeque<List<Stroke>>()

    fun addStroke(stroke: Stroke) {
        saveUndoState()
        strokes.add(stroke)
        redoStack.clear()
        updateState()
    }

    fun eraseAt(x: Float, y: Float) {
        val radius = 25f
        val removed = strokes.removeAll { stroke ->
            stroke.points.any { point ->
                val dx = point[0] - x
                val dy = point[1] - y
                dx * dx + dy * dy < radius * radius
            }
        }
        if (removed) {
            saveUndoState()
            redoStack.clear()
            updateState()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(strokes.toList())
        val previous = undoStack.removeLast()
        strokes.clear()
        strokes.addAll(previous)
        updateState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(strokes.toList())
        val next = redoStack.removeLast()
        strokes.clear()
        strokes.addAll(next)
        updateState()
    }

    fun setBrush(brush: Brush) {
        _state.value = _state.value.copy(currentBrush = brush, isEraser = false)
    }

    fun setColor(color: String) {
        _state.value = _state.value.copy(
            currentBrush = _state.value.currentBrush.copy(color = color)
        )
    }

    fun setSize(size: Float) {
        _state.value = _state.value.copy(
            currentBrush = _state.value.currentBrush.copy(size = size)
        )
    }

    fun toggleEraser() {
        _state.value = _state.value.copy(isEraser = !_state.value.isEraser)
    }

    private fun saveUndoState() {
        undoStack.addLast(strokes.toList())
        if (undoStack.size > 100) undoStack.removeFirst()
    }

    private fun updateState() {
        _state.value = _state.value.copy(
            strokes = strokes.toList(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun save() {
        viewModelScope.launch {
            // TODO: serialize strokes to note
        }
    }
}

class InkViewModelFactory(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InkViewModel(repository, noteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
