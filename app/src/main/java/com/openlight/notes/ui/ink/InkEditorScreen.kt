package com.openlight.notes.ui.ink

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer

/**
 * Ink editor screen: canvas + toolbar.
 */
@Composable
fun InkEditorScreen(
    noteId: String,
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: InkViewModel = viewModel(
        factory = InkViewModelFactory(container.repository, noteId)
    )
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        InkToolbar(
            currentBrush = state.currentBrush,
            isEraser = state.isEraser,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            onBrushSelected = viewModel::setBrush,
            onColorSelected = viewModel::setColor,
            onSizeChanged = viewModel::setSize,
            onEraserToggled = viewModel::toggleEraser,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onBack = onBack
        )

        InkCanvas(
            strokes = state.strokes,
            currentBrush = state.currentBrush,
            onStrokeFinished = viewModel::addStroke,
            onErase = viewModel::eraseAt,
            isEraser = state.isEraser,
            modifier = Modifier.fillMaxSize()
        )
    }
}
