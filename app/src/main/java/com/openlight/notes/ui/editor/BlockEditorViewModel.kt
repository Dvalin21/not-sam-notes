package com.openlight.notes.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openlight.notes.core.ink.Stroke
import com.openlight.notes.core.model.Block
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import com.openlight.notes.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class BlockItem(
    val id: String,
    val block: Block,
    val text: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val strokes: List<Stroke> = emptyList(),
    val imageFile: File? = null,
    val audioFile: File? = null,
    val audioDurationMs: Long = 0
)

data class BlockEditorState(
    val id: String = "",
    val title: String = "",
    val folder: String = "/",
    val blocks: List<BlockItem> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val pageTemplate: String = "blank"
)

class BlockEditorViewModel(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModel() {
    private val _state = MutableStateFlow(BlockEditorState())
    val state: StateFlow<BlockEditorState> = _state.asStateFlow()

    private val allStrokes = mutableListOf<Stroke>()
    private val undoStack = ArrayDeque<List<Stroke>>()
    private val redoStack = ArrayDeque<List<Stroke>>()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val entity = repository.getNote(noteId)
            if (entity != null) {
                val file = File(entity.filePath)
                val document = com.openlight.notes.core.container.NoteContainer.readDocument(file)
                val blocks = document.blocks.map { block ->
                    when (block) {
                        is Block.Text -> BlockItem(
                            id = block.id,
                            block = block,
                            text = block.text,
                            textFieldValue = TextFieldValue(block.text)
                        )
                        is Block.Ink -> {
                            val strokes = com.openlight.notes.core.container.NoteContainer.readStrokes(file, block.id)
                            allStrokes.addAll(strokes)
                            BlockItem(id = block.id, block = block, strokes = strokes)
                        }
                        is Block.Image -> BlockItem(id = block.id, block = block)
                        is Block.Audio -> BlockItem(id = block.id, block = block)
                        is Block.PdfPage -> BlockItem(id = block.id, block = block)
                    }
                }
                _state.value = _state.value.copy(
                    id = entity.id,
                    title = entity.title,
                    folder = entity.folder,
                    blocks = blocks,
                    strokes = allStrokes.toList(),
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun setTitle(title: String) {
        _state.value = _state.value.copy(title = title, isSaved = false)
    }

    fun addTextBlock(x: Float, y: Float) {
        val id = UUID.randomUUID().toString()
        val block = Block.Text(id = id, text = "", x = x, y = y)
        val item = BlockItem(id = id, block = block, text = "")
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + item,
            isSaved = false
        )
    }

    fun addInkBlock() {
        val id = UUID.randomUUID().toString()
        val block = Block.Ink(id = id, height = 900f)
        val item = BlockItem(id = id, block = block)
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + item,
            isSaved = false
        )
    }

    fun addImageBlock(x: Float, y: Float) {
        val id = UUID.randomUUID().toString()
        val block = Block.Image(id = id, media = "", x = x, y = y)
        val item = BlockItem(id = id, block = block)
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + item,
            isSaved = false
        )
    }

    fun addAudioBlock() {
        val id = UUID.randomUUID().toString()
        val block = Block.Audio(id = id, media = "", durMs = 0)
        val item = BlockItem(id = id, block = block)
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + item,
            isSaved = false
        )
    }

    fun updateTextBlock(blockId: String, newText: String, newValue: TextFieldValue) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { item ->
                if (item.id == blockId && item.block is Block.Text) {
                    item.copy(
                        text = newText,
                        textFieldValue = newValue,
                        block = (item.block as Block.Text).copy(text = newText)
                    )
                } else item
            },
            isSaved = false
        )
    }

    fun moveTextBlock(blockId: String, x: Float, y: Float) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { item ->
                if (item.id == blockId && item.block is Block.Text) {
                    item.copy(block = (item.block as Block.Text).copy(x = x, y = y))
                } else item
            },
            isSaved = false
        )
    }

    fun moveImageBlock(blockId: String, x: Float, y: Float) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { item ->
                if (item.id == blockId && item.block is Block.Image) {
                    item.copy(block = (item.block as Block.Image).copy(x = x, y = y))
                } else item
            },
            isSaved = false
        )
    }

    fun resizeImageBlock(blockId: String, w: Float, h: Float) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { item ->
                if (item.id == blockId && item.block is Block.Image) {
                    item.copy(block = (item.block as Block.Image).copy(displayW = w, displayH = h))
                } else item
            },
            isSaved = false
        )
    }

    fun addStroke(stroke: Stroke) {
        saveUndoState()
        allStrokes.add(stroke)
        redoStack.clear()
        _state.value = _state.value.copy(
            strokes = allStrokes.toList(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            isSaved = false
        )
    }

    fun eraseAt(x: Float, y: Float) {
        val radius = 25f
        val removed = allStrokes.removeAll { stroke ->
            stroke.points.any { point ->
                val dx = point[0] - x
                val dy = point[1] - y
                dx * dx + dy * dy < radius * radius
            }
        }
        if (removed) {
            saveUndoState()
            redoStack.clear()
            _state.value = _state.value.copy(
                strokes = allStrokes.toList(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isSaved = false
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(allStrokes.toList())
        val previous = undoStack.removeLast()
        allStrokes.clear()
        allStrokes.addAll(previous)
        _state.value = _state.value.copy(
            strokes = allStrokes.toList(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(allStrokes.toList())
        val next = redoStack.removeLast()
        allStrokes.clear()
        allStrokes.addAll(next)
        _state.value = _state.value.copy(
            strokes = allStrokes.toList(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun setPageTemplate(template: String) {
        _state.value = _state.value.copy(pageTemplate = template, isSaved = false)
    }

    fun deleteBlock(blockId: String) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.filter { it.id != blockId },
            isSaved = false
        )
    }

    private fun saveUndoState() {
        undoStack.addLast(allStrokes.toList())
        if (undoStack.size > 100) undoStack.removeFirst()
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val entity = repository.getNote(noteId) ?: return@launch
            val file = File(entity.filePath)
            val existingManifest = com.openlight.notes.core.container.NoteContainer.readManifest(file)
            val manifest = existingManifest.copy(title = s.title)
            val blocks = s.blocks.map { it.block }
            val document = Document(blocks = blocks)
            repository.saveNote(noteId, manifest, document)

            // Save all canvas strokes to a synthetic ink block
            if (allStrokes.isNotEmpty()) {
                val inkBlockId = s.blocks.firstOrNull { it.block is Block.Ink }?.id
                    ?: "canvas_strokes_${noteId}"
                com.openlight.notes.core.container.NoteContainer.writeStrokes(file, inkBlockId, allStrokes.toList())
            }

            _state.value = _state.value.copy(isSaved = true)
        }
    }
}

class BlockEditorViewModelFactory(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlockEditorViewModel::class.java)) {
            return BlockEditorViewModel(repository, noteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}