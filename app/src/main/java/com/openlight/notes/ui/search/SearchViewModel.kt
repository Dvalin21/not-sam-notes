package com.openlight.notes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openlight.notes.repository.NoteRepository
import com.openlight.notes.core.search.SearchEngine
import com.openlight.notes.core.search.SearchResult
import com.openlight.notes.core.search.MatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: List<SearchResult> = emptyList(),
    val query: String = "",
    val isSearching: Boolean = false
)

class SearchViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val searchEngine = SearchEngine()

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), query = "")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, query = query)

            val allNotes = repository.search(query)
            val results = allNotes.map { note ->
                SearchResult(
                    noteId = note.id,
                    noteTitle = note.title,
                    snippet = searchEngine.makeSnippet(note.title, query),
                    matchType = MatchType.TITLE
                )
            }

            _state.value = _state.value.copy(
                results = results,
                isSearching = false
            )
        }
    }
}

class SearchViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
