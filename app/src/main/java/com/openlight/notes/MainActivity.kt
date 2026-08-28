package com.openlight.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.ui.NotesViewModel
import com.openlight.notes.ui.NotesViewModelFactory
import com.openlight.notes.ui.editor.BlockEditorScreen
import com.openlight.notes.ui.folders.FoldersScreen
import com.openlight.notes.ui.search.SearchScreen
import com.openlight.notes.ui.settings.SettingsScreen
import com.openlight.notes.ui.sync.SyncScreen

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

enum class AppScreen {
    NOTES_LIST, EDITOR, FOLDERS, SEARCH, SYNC, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesApp() {
    val context = LocalContext.current
    val container = remember { AppContainer(context) }
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(container.repository))

    val notes by viewModel.notes.collectAsState(initial = emptyList())
    var currentScreen by remember { mutableStateOf(AppScreen.NOTES_LIST) }
    var openNoteId by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        AppScreen.EDITOR -> {
            openNoteId?.let { noteId ->
                BlockEditorScreen(
                    noteId = noteId,
                    container = container,
                    onBack = {
                        openNoteId = null
                        currentScreen = AppScreen.NOTES_LIST
                    }
                )
            }
        }
        AppScreen.FOLDERS -> {
            FoldersScreen(
                container = container,
                onBack = { currentScreen = AppScreen.NOTES_LIST },
                onNoteClick = { id ->
                    openNoteId = id
                    currentScreen = AppScreen.EDITOR
                }
            )
        }
        AppScreen.SEARCH -> {
            SearchScreen(
                container = container,
                onBack = { currentScreen = AppScreen.NOTES_LIST },
                onNoteClick = { id ->
                    openNoteId = id
                    currentScreen = AppScreen.EDITOR
                }
            )
        }
        AppScreen.SYNC -> {
            SyncScreen(
                container = container,
                onBack = { currentScreen = AppScreen.NOTES_LIST }
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                container = container,
                onBack = { currentScreen = AppScreen.NOTES_LIST }
            )
        }
        AppScreen.NOTES_LIST -> {
            NotesListScreen(
                notes = notes,
                onNoteClick = { id ->
                    openNoteId = id
                    currentScreen = AppScreen.EDITOR
                },
                onFoldersClick = { currentScreen = AppScreen.FOLDERS },
                onSearchClick = { currentScreen = AppScreen.SEARCH },
                onSyncClick = { currentScreen = AppScreen.SYNC },
                onSettingsClick = { currentScreen = AppScreen.SETTINGS },
                onCreateNote = {
                    val id = viewModel.createNote()
                    openNoteId = id
                    currentScreen = AppScreen.EDITOR
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListScreen(
    notes: List<com.openlight.notes.db.NoteEntity>,
    onNoteClick: (String) -> Unit,
    onFoldersClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateNote: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Not Sam Notes", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onFoldersClick) {
                        Icon(Icons.Default.Folder, contentDescription = "Folders")
                    }
                    IconButton(onClick = onSyncClick) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
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
                            .clickable { onNoteClick(note.id) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = note.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = note.folder,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
