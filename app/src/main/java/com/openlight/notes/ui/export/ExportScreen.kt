package com.openlight.notes.ui.export

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.openlight.notes.AppContainer
import com.openlight.notes.core.model.Document
import com.openlight.notes.core.model.NoteManifest
import com.openlight.notes.export.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Export screen (AD-12): format selection and share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exporting by remember { mutableStateOf(false) }
    var lastExportFile by remember { mutableStateOf<File?>(null) }
    var selectedFormat by remember { mutableStateOf("PDF") }

    val formats = listOf("PDF", "PNG", "TXT", "DOCX", "PPTX")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            formats.forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Button(
                        onClick = { selectedFormat = format },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = format,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        exporting = true
                        withContext(Dispatchers.IO) {
                            exportNote(context, container, selectedFormat) { file ->
                                lastExportFile = file
                            }
                        }
                        exporting = false
                    }
                },
                enabled = !exporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = if (exporting) "Exporting..." else "Export as $selectedFormat",
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            lastExportFile?.let { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exported: ${file.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                shareFile(context, file)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportNote(
    context: Context,
    container: AppContainer,
    format: String,
    onResult: (File?) -> Unit
) {
    try {
        val notesDir = container.notesDir
        val noteFiles = notesDir.listFiles { file -> file.extension == "note" }
        if (noteFiles.isNullOrEmpty()) {
            onResult(null)
            return
        }

        val noteFile = noteFiles.first()
        val noteId = noteFile.nameWithoutExtension
        val entity = container.repository.getNote(noteId) ?: run {
            onResult(null)
            return
        }

        val manifest = NoteManifest(
            id = entity.id,
            title = entity.title,
            folder = entity.folder,
            created = entity.created,
            modified = entity.modified
        )

        // Read document from file
        val document = try {
            com.openlight.notes.core.container.NoteContainer.read(noteFile).second
        } catch (e: Exception) {
            Document()
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val title = entity.title.ifEmpty { "note" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val outputFile = when (format) {
            "PDF" -> {
                val file = File(exportDir, "$title.pdf")
                if (Exporter.exportPdf(context, manifest, document, file)) file else null
            }
            "PNG" -> {
                val file = File(exportDir, "$title.png")
                if (Exporter.exportPng(manifest, document, file)) file else null
            }
            "TXT" -> {
                val file = File(exportDir, "$title.txt")
                if (Exporter.exportTxt(manifest, document, file)) file else null
            }
            "DOCX" -> {
                val file = File(exportDir, "$title.docx")
                if (Exporter.exportDocx(manifest, document, file)) file else null
            }
            "PPTX" -> {
                val file = File(exportDir, "$title.pptx")
                if (Exporter.exportPptx(manifest, document, file)) file else null
            }
            else -> null
        }

        onResult(outputFile)
    } catch (e: Exception) {
        onResult(null)
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "txt" -> "text/plain"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    } catch (e: Exception) {
        // Fallback without FileProvider
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TEXT, file.absolutePath)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }
}