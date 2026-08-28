package com.openlight.notes.ui.export

import android.media.MediaPlayer
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
import com.openlight.notes.audio.AudioPlayer
import com.openlight.notes.audio.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AudioRecorderScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var currentTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val player = remember { AudioPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                AudioRecorder.stopRecording()
            }
            player.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Audio Recorder",
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (recordedFile != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recorded: ${recordedFile?.name ?: "audio"}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isPlaying) {
                        IconButton(onClick = {
                            player.stop()
                            isPlaying = false
                            currentTime = 0L
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                        Text(
                            text = "Playing: ${currentTime / 1000}s / ${duration / 1000}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        IconButton(onClick = {
                            recordedFile?.let { file ->
                                try {
                                    player.play(file) {
                                        isPlaying = false
                                        currentTime = 0L
                                    }
                                    isPlaying = true
                                    duration = player.getDuration().toLong()
                                    scope.launch {
                                        while (player.isPlaying()) {
                                            currentTime = player.getCurrentPosition().toLong()
                                            delay(100L)
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                }
                            }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { recordedFile = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", maxLines = 1)
                }

                Button(
                    onClick = { onBack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", maxLines = 1)
                }
            }
        } else {
            if (isRecording) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recording...", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${currentTime / 1000}s",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            val file = AudioRecorder.stopRecording()
                            isRecording = false
                            if (file != null) recordedFile = file
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Recording", maxLines = 1)
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tap to start recording",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = {
                            try {
                                val file = AudioRecorder.startRecording(context)
                                isRecording = true
                                currentTime = 0L
                                scope.launch {
                                    while (isRecording) {
                                        delay(1000L)
                                        currentTime += 1000L
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording", maxLines = 1)
                        }
                    }
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
