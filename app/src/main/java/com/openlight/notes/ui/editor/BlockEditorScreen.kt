package com.openlight.notes.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer
import com.openlight.notes.ui.text.TextEditorViewModel
import com.openlight.notes.ui.text.TextEditorViewModelFactory

/**
 * Block editor screen: supports text, ink, images, audio (AD-2).
 * Phase 3: text blocks with title editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorScreen(
    noteId: String,
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: TextEditorViewModel = viewModel(
        factory = TextEditorViewModelFactory(container.repository, noteId)
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        placeholder = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.save()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = state.text,
                onValueChange = viewModel::setText,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Start typing...") }
            )
        }
    }
}
