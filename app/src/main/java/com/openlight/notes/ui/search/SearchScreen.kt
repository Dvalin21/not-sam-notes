package com.openlight.notes.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer
import com.openlight.notes.core.search.SearchResult
import com.openlight.notes.core.search.MatchType

/**
 * Search screen: full-text search over typed text + handwriting recognition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(container.repository)
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            var query by remember { mutableStateOf("") }

            TextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                placeholder = { Text("Search notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            LazyColumn {
                items(state.results) { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(result.noteId) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = result.noteTitle.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = result.snippet,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = when (result.matchType) {
                                MatchType.TITLE -> "Title"
                                MatchType.TEXT -> "Text"
                                MatchType.HANDWRITING -> "Handwriting"
                                else -> "Other"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
