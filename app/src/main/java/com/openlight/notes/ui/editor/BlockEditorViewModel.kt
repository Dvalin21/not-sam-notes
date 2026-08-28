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
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

class BlockEditorViewModel(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModel() {
    private val _state = MutableStateFlow(BlockEditorState())
    val state: StateFlow<BlockEditorState> = _state.asStateFlow()

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

    fun addTextBlock() {
        val id = UUID.randomUUID().toString()
        val block = Block.Text(id = id, text = "")
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

    fun addImageBlock() {
        val id = UUID.randomUUID().toString()
        val block = Block.Image(id = id, media = "")
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

    fun updateInkStrokes(blockId: String, strokes: List<Stroke>) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { item ->
                if (item.id == blockId) item.copy(strokes = strokes) else item
            },
            isSaved = false
        )
    }

    fun deleteBlock(blockId: String) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.filter { it.id != blockId },
            isSaved = false
        )
    }

    fun moveBlockUp(index: Int) {
        if (index <= 0) return
        val blocks = _state.value.blocks.toMutableList()
        val item = blocks.removeAt(index)
        blocks.add(index - 1, item)
        _state.value = _state.value.copy(blocks = blocks, isSaved = false)
    }

    fun moveBlockDown(index: Int) {
        val blocks = _state.value.blocks
        if (index >= blocks.size - 1) return
        val mutable = blocks.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(index + 1, item)
        _state.value = _state.value.copy(blocks = mutable, isSaved = false)
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
            
            // Save ink strokes separately
            s.blocks.forEach { item ->
                if (item.block is Block.Ink && item.strokes.isNotEmpty()) {
                    com.openlight.notes.core.container.NoteContainer.writeStrokes(file, item.id, item.strokes)
                }
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