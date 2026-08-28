package com.openlight.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.ui.NotesViewModel
import com.openlight.notes.ui.NotesViewModelFactory
import com.openlight.notes.ui.editor.BlockEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotesApp()
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotesApp() {
    val context = LocalContext.current
    val container = remember { AppContainer(context) }
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(container.repository))

    val notes by viewModel.notes.collectAsState(initial = emptyList())
    var openNoteId by remember { mutableStateOf<String?>(null) }

    val currentNoteId = openNoteId
    if (currentNoteId != null) {
        BlockEditorScreen(
            noteId = currentNoteId,
            container = container,
            onBack = { openNoteId = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Not Sam Notes") },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Add, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val id = viewModel.createNote()
                openNoteId = id
            }) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Text(
                text = "No notes yet. Tap + to create one.",
                modifier = Modifier.padding(padding).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(notes) { note ->
                    Column(
                        modifier = Modifier
                            .clickable { openNoteId = note.id }
                            .padding(16.dp)
                    ) {
                        Text(text = note.title.ifEmpty { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                        Text(text = note.folder, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
