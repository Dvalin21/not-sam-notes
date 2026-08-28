package com.openlight.notes.ui.export

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openlight.notes.AppContainer
import com.openlight.notes.core.model.Block
import com.openlight.notes.pdf.PdfImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF import screen (Phase 9): file picker → PdfImporter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfImportScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<List<Block.PdfPage>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileName = getFileName(context, it) ?: "selected.pdf"
            importResult = null
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import PDF") },
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
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("application/pdf"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Text(
                    text = "Select PDF File",
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            selectedFileName?.let { name ->
                Text(
                    text = "Selected: $name",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        importing = true
                        errorMessage = null
                        try {
                            withContext(Dispatchers.IO) {
                                selectedUri?.let { uri ->
                                    val mediaDir = File(container.notesDir, "pdf_imports").apply { mkdirs() }
                                    importResult = PdfImporter.importPdf(context, uri, mediaDir)
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Import failed"
                        } finally {
                            importing = false
                        }
                    }
                },
                enabled = selectedUri != null && !importing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = if (importing) "Importing..." else "Import PDF",
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            if (importing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }

            importResult?.let { blocks ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Import Successful",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${blocks.size} page(s) imported",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Import Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}