package com.openlight.notes.ui.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openlight.notes.AppContainer
import com.openlight.notes.db.NoteEntity
import com.openlight.notes.ui.NotesViewModel
import com.openlight.notes.ui.NotesViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(container.repository))
    val notes by viewModel.notes.collectAsState(initial = emptyList())

    var showTrash by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val filteredNotes = when {
        showTrash -> notes.filter { it.trashed }
        showFavorites -> notes.filter { it.favorite }
        selectedFolder != null -> notes.filter { it.folder == selectedFolder && !it.trashed }
        else -> notes.filter { !it.trashed }
    }

    val folders = notes.map { it.folder }.distinct().sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            showTrash -> "Trash"
                            showFavorites -> "Favorites"
                            selectedFolder != null -> selectedFolder!!
                            else -> "Notes"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = if (isGridView) "List view" else "Grid view"
                        )
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
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showTrash && !showFavorites && selectedFolder == null,
                    onClick = {
                        showTrash = false
                        showFavorites = false
                        selectedFolder = null
                    },
                    label = { Text("All", maxLines = 1) }
                )
                FilterChip(
                    selected = showFavorites,
                    onClick = {
                        showFavorites = !showFavorites
                        showTrash = false
                        selectedFolder = null
                    },
                    label = { Text("Favorites", maxLines = 1) }
                )
                FilterChip(
                    selected = showTrash,
                    onClick = {
                        showTrash = !showTrash
                        showFavorites = false
                        selectedFolder = null
                    },
                    label = { Text("Trash", maxLines = 1) }
                )
            }

            // Folder list (only in list view mode and not in trash/favorites)
            if (!isGridView && !showTrash && !showFavorites) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folder }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${notes.count { it.folder == folder && !it.trashed }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HorizontalDivider()
            }

            // Notes grid/list
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteGridItem(
                            note = note,
                            onClick = { onNoteClick(note.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteListItem(
                            note = note,
                            onClick = { onNoteClick(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteGridItem(note: NoteEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.folder,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NoteListItem(note: NoteEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    note.locked -> Icons.Default.Lock
                    note.favorite -> Icons.Default.Star
                    else -> Icons.Default.Description
                },
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
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
