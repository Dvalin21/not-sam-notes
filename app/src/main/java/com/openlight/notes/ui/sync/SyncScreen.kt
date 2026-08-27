package com.openlight.notes.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer
import com.openlight.notes.core.sync.SyncResult

/**
 * Sync status screen: per-target status, manual sync, error display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    container: AppContainer
) {
    val viewModel: SyncViewModel = viewModel(
        factory = SyncViewModelFactory(container.repository)
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync & Backup") },
                actions = {
                    IconButton(onClick = { viewModel.syncAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync all")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(state.targets) { target ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = target.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (target.isConnected) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = "Connected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Disconnected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Text(
                            text = target.type.name,
                            style = MaterialTheme.typography.bodySmall
                        )

                        target.lastResult?.let { result ->
                            Text(
                                text = "↑ ${result.uploaded}  ↓ ${result.downloaded}  ⚡ ${result.conflicts}  ✗ ${result.errors}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (target.isSyncing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }

                        target.errorMessage?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
