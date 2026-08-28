package com.openlight.notes.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openlight.notes.repository.NoteRepository
import com.openlight.notes.sync.SyncResult
import com.openlight.notes.sync.SyncTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TargetUiState(
    val id: String,
    val name: String,
    val type: String,
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val lastResult: SyncResult? = null,
    val errorMessage: String? = null
)

data class SyncUiState(
    val targets: List<TargetUiState> = emptyList(),
    val isSyncing: Boolean = false
)

class SyncViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    fun syncAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true)
            // TODO: implement sync
            _state.value = _state.value.copy(isSyncing = false)
        }
    }

    fun addTarget(target: SyncTarget) {
        // TODO: add target
    }

    fun removeTarget(targetId: String) {
        // TODO: remove target
    }
}

class SyncViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SyncViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
