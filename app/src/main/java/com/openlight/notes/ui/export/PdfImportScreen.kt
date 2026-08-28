package com.openlight.notes.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.openlight.notes.pdf.PdfImporter
import java.io.File

@Composable
fun PdfImportScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mediaDir = remember { File(context.filesDir, "pdf_import_media").also { it.mkdirs() } }

    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importing by remember { mutableStateOf(false) }
    var pageCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { pdfUri = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Import PDF",
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                picker.launch(arrayOf("application/pdf"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick PDF to import", maxLines = 1)
        }

        pdfUri?.let { uri ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selected: ${uri.lastPathSegment ?: uri.toString()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (importing) return@Button
                            importing = true
                            errorMessage = null
                            try {
                                val blocks = PdfImporter.importPdf(context, uri, mediaDir)
                                pageCount = blocks.size
                                importing = false
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Import failed"
                                importing = false
                            }
                        },
                        enabled = !importing
                    ) {
                        if (importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing...", maxLines = 1)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import PDF", maxLines = 1)
                        }
                    }

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    if (pageCount > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Imported $pageCount page${if (pageCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Saved to ${mediaDir.path}",
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
