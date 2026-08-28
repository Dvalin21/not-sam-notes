package com.openlight.notes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer
import com.openlight.notes.core.model.Block
import com.openlight.notes.core.refinement.HandwritingRefinement
import com.openlight.notes.ui.audio.AudioBlock
import com.openlight.notes.ui.ink.InkCanvas
import com.openlight.notes.ui.text.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorScreen(
    noteId: String,
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel: BlockEditorViewModel = viewModel(
        factory = BlockEditorViewModelFactory(container.repository, noteId)
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
        },
        bottomBar = {
            BlockToolbar(
                onAddText = viewModel::addTextBlock,
                onAddInk = viewModel::addInkBlock,
                onAddImage = viewModel::addImageBlock,
                onAddAudio = viewModel::addAudioBlock
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = state.blocks,
                    key = { _, item -> item.id }
                ) { index, item ->
                    BlockCard(
                        blockItem = item,
                        index = index,
                        totalCount = state.blocks.size,
                        onTextChange = { text, value ->
                            viewModel.updateTextBlock(item.id, text, value)
                        },
                        onStrokesChange = { strokes ->
                            viewModel.updateInkStrokes(item.id, strokes)
                        },
                        onDelete = { viewModel.deleteBlock(item.id) },
                        onMoveUp = { viewModel.moveBlockUp(index) },
                        onMoveDown = { viewModel.moveBlockDown(index) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BlockToolbar(
    onAddText: () -> Unit,
    onAddInk: () -> Unit,
    onAddImage: () -> Unit,
    onAddAudio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onAddText) {
            Icon(Icons.Default.TextFields, contentDescription = "Add text")
        }
        IconButton(onClick = onAddInk) {
            Icon(Icons.Default.Edit, contentDescription = "Add ink")
        }
        IconButton(onClick = onAddImage) {
            Icon(Icons.Default.Image, contentDescription = "Add image")
        }
        IconButton(onClick = onAddAudio) {
            Icon(Icons.Default.Mic, contentDescription = "Add audio")
        }
    }
}

@Composable
private fun BlockCard(
    blockItem: BlockItem,
    index: Int,
    totalCount: Int,
    onTextChange: (String, TextFieldValue) -> Unit,
    onStrokesChange: (List<com.openlight.notes.core.ink.Stroke>) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BlockControls(
                index = index,
                totalCount = totalCount,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete
            )
            when (val block = blockItem.block) {
                is Block.Text -> {
                    RichTextEditor(
                        value = blockItem.textFieldValue,
                        onValueChange = { onTextChange(it.text, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                is Block.Ink -> {
                    val strokes = remember(blockItem.id) {
                        mutableStateListOf<com.openlight.notes.core.ink.Stroke>().apply {
                            addAll(blockItem.strokes)
                        }
                    }
                    var template by remember { mutableStateOf("blank") }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Template selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("blank", "lines", "grid", "dots").forEach { t ->
                                FilterChip(
                                    selected = template == t,
                                    onClick = { template = t },
                                    label = { Text(t, maxLines = 1) }
                                )
                            }
                        }
                        // Refinement toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    val refined = HandwritingRefinement.straighten(strokes.toList())
                                    strokes.clear()
                                    strokes.addAll(refined)
                                    onStrokesChange(strokes.toList())
                                },
                                enabled = strokes.isNotEmpty()
                            ) {
                                Text("Straighten", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    val refined = HandwritingRefinement.tidy(strokes.toList())
                                    strokes.clear()
                                    strokes.addAll(refined)
                                    onStrokesChange(strokes.toList())
                                },
                                enabled = strokes.isNotEmpty()
                            ) {
                                Text("Tidy", maxLines = 1)
                            }
                        }
                        InkCanvas(
                            strokes = strokes,
                            currentBrush = com.openlight.notes.core.ink.Brush(),
                            onStrokeFinished = { stroke ->
                                strokes.add(stroke)
                                onStrokesChange(strokes.toList())
                            },
                            onErase = { /* erase handled internally */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(8.dp),
                            pageTemplate = template
                        )
                    }
                }
                is Block.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.LightGray)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Image block", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is Block.Audio -> {
                    AudioBlock(
                        file = blockItem.audioFile,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                is Block.PdfPage -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.LightGray)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PDF block", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockControls(
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Move up",
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Move down",
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
