package com.openlight.notes.ui.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openlight.notes.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TextEditorState(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val folder: String = "/",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

class TextEditorViewModel(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModel() {
    private val _state = MutableStateFlow(TextEditorState())
    val state: StateFlow<TextEditorState> = _state.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val entity = repository.getNote(noteId)
            if (entity != null) {
                _state.value = _state.value.copy(
                    id = entity.id,
                    title = entity.title,
                    folder = entity.folder,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun setText(text: String) {
        _state.value = _state.value.copy(text = text, isSaved = false)
    }

    fun setTitle(title: String) {
        _state.value = _state.value.copy(title = title, isSaved = false)
    }

    fun toggleBold() { _state.value = _state.value.copy(isBold = !_state.value.isBold) }
    fun toggleItalic() { _state.value = _state.value.copy(isItalic = !_state.value.isItalic) }
    fun toggleUnderline() { _state.value = _state.value.copy(isUnderline = !_state.value.isUnderline) }
    fun toggleStrikethrough() { _state.value = _state.value.copy(isStrikethrough = !_state.value.isStrikethrough) }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}

class TextEditorViewModelFactory(
    private val repository: NoteRepository,
    private val noteId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TextEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TextEditorViewModel(repository, noteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
