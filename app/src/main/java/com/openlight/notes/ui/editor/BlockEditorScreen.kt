package com.openlight.notes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlight.notes.AppContainer
import com.openlight.notes.core.ink.Brush
import com.openlight.notes.core.model.Block
import com.openlight.notes.core.refinement.HandwritingRefinement
import com.openlight.notes.ui.ink.InkCanvas
import kotlin.math.roundToInt

private enum class CanvasTool {
    PEN, MARKER, HIGHLIGHTER, ERASER, TEXT, IMAGE
}

private data class ToolState(
    val tool: CanvasTool = CanvasTool.PEN,
    val brush: Brush = Brush()
)

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

    var toolState by remember { mutableStateOf(ToolState()) }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

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
            Column {
                // Color picker popup
                if (showColorPicker) {
                    ColorPalette(
                        currentColor = toolState.brush.color,
                        onColorSelected = { color ->
                            toolState = toolState.copy(brush = toolState.brush.copy(color = color))
                            showColorPicker = false
                        }
                    )
                }

                // Size slider (visible when drawing tool active)
                if (toolState.tool in listOf(CanvasTool.PEN, CanvasTool.MARKER, CanvasTool.HIGHLIGHTER)) {
                    Slider(
                        value = toolState.brush.size,
                        onValueChange = { size ->
                            toolState = toolState.copy(brush = toolState.brush.copy(size = size))
                        },
                        valueRange = 1f..25f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Main toolbar
                SamsungStyleToolbar(
                    toolState = toolState,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onToolSelected = { tool ->
                        toolState = toolState.copy(tool = tool)
                        showColorPicker = tool in listOf(CanvasTool.PEN, CanvasTool.MARKER, CanvasTool.HIGHLIGHTER)
                    },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )
            }
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
            // Paper-like canvas with shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
            ) {
                InkCanvas(
                    strokes = state.strokes,
                    currentBrush = toolState.brush,
                    onStrokeFinished = { stroke ->
                        viewModel.addStroke(stroke)
                    },
                    onErase = { offset ->
                        viewModel.eraseAt(offset.x, offset.y)
                    },
                    isEraser = toolState.tool == CanvasTool.ERASER,
                    isPlacementMode = toolState.tool == CanvasTool.TEXT || toolState.tool == CanvasTool.IMAGE,
                    pageTemplate = state.pageTemplate,
                    modifier = Modifier.fillMaxSize(),
                    onCanvasTap = { offset ->
                        when (toolState.tool) {
                            CanvasTool.TEXT -> viewModel.addTextBlock(offset.x, offset.y)
                            CanvasTool.IMAGE -> viewModel.addImageBlock(offset.x, offset.y)
                            else -> {}
                        }
                    }
                )

                // Floating text blocks
                state.blocks.filter { it.block is Block.Text }.forEach { item ->
                    val block = item.block as Block.Text
                    FloatingTextBlock(
                        blockItem = block,
                        isSelected = selectedBlockId == block.id,
                        onSelect = { selectedBlockId = block.id },
                        onMove = { dx, dy -> viewModel.moveTextBlock(block.id, dx, dy) },
                        onTextChange = { text -> viewModel.updateTextBlock(block.id, text, TextFieldValue(text)) },
                        onDelete = {
                            viewModel.deleteBlock(block.id)
                            if (selectedBlockId == block.id) selectedBlockId = null
                        }
                    )
                }

                // Floating image blocks
                state.blocks.filter { it.block is Block.Image }.forEach { item ->
                    val block = item.block as Block.Image
                    FloatingImageBlock(
                        block = block,
                        isSelected = selectedBlockId == block.id,
                        onSelect = { selectedBlockId = block.id },
                        onMove = { dx, dy -> viewModel.moveImageBlock(block.id, dx, dy) },
                        onResize = { w, h -> viewModel.resizeImageBlock(block.id, w, h) },
                        onDelete = {
                            viewModel.deleteBlock(block.id)
                            if (selectedBlockId == block.id) selectedBlockId = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SamsungStyleToolbar(
    toolState: ToolState,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (CanvasTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pen
        ToolIconButton(
            icon = Icons.Default.Brush,
            isSelected = toolState.tool == CanvasTool.PEN,
            onClick = { onToolSelected(CanvasTool.PEN) }
        )
        // Marker
        ToolIconButton(
            icon = Icons.Outlined.Circle,
            isSelected = toolState.tool == CanvasTool.MARKER,
            onClick = { onToolSelected(CanvasTool.MARKER) }
        )
        // Highlighter
        ToolIconButton(
            icon = Icons.Default.Edit,
            isSelected = toolState.tool == CanvasTool.HIGHLIGHTER,
            onClick = { onToolSelected(CanvasTool.HIGHLIGHTER) }
        )
        // Eraser
        ToolIconButton(
            icon = Icons.Default.Edit, // TODO: eraser icon
            isSelected = toolState.tool == CanvasTool.ERASER,
            onClick = { onToolSelected(CanvasTool.ERASER) }
        )
        // Text
        ToolIconButton(
            icon = Icons.Default.TextFields,
            isSelected = toolState.tool == CanvasTool.TEXT,
            onClick = { onToolSelected(CanvasTool.TEXT) }
        )
        // Image
        ToolIconButton(
            icon = Icons.Default.Image,
            isSelected = toolState.tool == CanvasTool.IMAGE,
            onClick = { onToolSelected(CanvasTool.IMAGE) }
        )
        // Undo
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.Default.Undo, contentDescription = "Undo")
        }
        // Redo
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.Default.Redo, contentDescription = "Redo")
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ColorPalette(
    currentColor: String,
    onColorSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val colors = listOf("#1A1A1A", "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA")
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(color)))
                    .border(
                        width = if (currentColor == color) 3.dp else 1.dp,
                        color = if (currentColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun FloatingTextBlock(
    blockItem: Block.Text,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset { IntOffset(blockItem.x.roundToInt(), blockItem.y.roundToInt()) }
            .width(with(density) { blockItem.width.toDp() })
            .height(with(density) { blockItem.height.toDp() })
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .pointerInput(blockItem.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(blockItem.x + dragAmount.x, blockItem.y + dragAmount.y)
                }
            }
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = blockItem.text.ifEmpty { "Tap to type" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (blockItem.text.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FloatingImageBlock(
    block: Block.Image,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset { IntOffset(block.x.roundToInt(), block.y.roundToInt()) }
            .size(with(density) { block.displayW.toDp() }, with(density) { block.displayH.toDp() })
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .pointerInput(block.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(block.x + dragAmount.x, block.y + dragAmount.y)
                }
            }
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Image", style = MaterialTheme.typography.bodySmall)
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FloatingAudioBlock(block: Block.Audio) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Edit, contentDescription = "Audio", tint = Color.White)
    }
}
