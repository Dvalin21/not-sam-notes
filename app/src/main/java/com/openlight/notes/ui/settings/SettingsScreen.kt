package com.openlight.notes.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openlight.notes.AppContainer
import com.openlight.notes.security.AppLock
import com.openlight.notes.ui.NotesViewModel
import com.openlight.notes.ui.NotesViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(container.repository))
    val notes by viewModel.notes.collectAsState(initial = emptyList())

    var appLockEnabled by remember { mutableStateOf(AppLock.isLockEnabled(context)) }
    var biometricAvailable by remember { mutableStateOf(AppLock.isBiometricAvailable(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLockNotes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Lock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App Lock", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (biometricAvailable) "Enable biometric lock" else "Biometric not available",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = {
                                    appLockEnabled = it
                                    AppLock.setLockEnabled(context, it)
                                },
                                enabled = biometricAvailable
                            )
                        }
                    }
                }
            }

            // Locked Notes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLockNotes = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Locked Notes", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                "${notes.count { it.locked }} locked",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // Theme
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showThemeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Theme", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text("System default", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // App Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App Info", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Not Sam Notes", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text("v0.1.0 — Personal build", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text("Package: com.openlight.notes", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    Text("System default")
                    Text("Light")
                    Text("Dark")
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLockNotes) {
        LockNotesDialog(
            lockedNotes = notes.filter { it.locked },
            container = container,
            onDismiss = { showLockNotes = false }
        )
    }
}

@Composable
private fun LockNotesDialog(
    lockedNotes: List<com.openlight.notes.db.NoteEntity>,
    container: AppContainer,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Locked Notes") },
        text = {
            if (lockedNotes.isEmpty()) {
                Text("No locked notes")
            } else {
                LazyColumn {
                    items(lockedNotes) { note ->
                        Text(note.title.ifEmpty { "Untitled" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
