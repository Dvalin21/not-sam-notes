package com.openlight.notes.ui.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer

/**
 * Text note editor screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
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
                title = { Text(state.title.ifEmpty { "Untitled" }) },
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
            RichTextToolbar(
                onBold = { viewModel.toggleBold() },
                onItalic = { viewModel.toggleItalic() },
                onUnderline = { viewModel.toggleUnderline() },
                onStrikethrough = { viewModel.toggleStrikethrough() },
                onColor = { },
                onHighlight = { },
                onHeading = { },
                onBulletList = { },
                onNumberedList = { },
                onCheckList = { }
            )

            TextField(
                value = state.text,
                onValueChange = { viewModel.setText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Start typing...") }
            )
        }
    }
}
