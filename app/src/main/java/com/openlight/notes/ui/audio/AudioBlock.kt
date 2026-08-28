package com.openlight.notes.ui.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openlight.notes.audio.AudioPlayer
import com.openlight.notes.audio.AudioRecorder
import java.io.File

/**
 * Audio recording/playback UI (Phase 9).
 */
@Composable
fun AudioBlock(
    file: File?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(AudioRecorder.isRecording()) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(file?.let { AudioPlayer().apply { play(it) } }?.getDuration() ?: 0) }

    val audioPlayer = remember { AudioPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRecording) {
            IconButton(onClick = {
                AudioRecorder.stopRecording()
                isRecording = false
            }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
            Text("Recording...", style = MaterialTheme.typography.bodySmall)
        } else {
            IconButton(onClick = {
                AudioRecorder.startRecording(context)
                isRecording = true
            }) {
                Icon(Icons.Default.Mic, contentDescription = "Record")
            }

            if (file != null && file.exists()) {
                IconButton(onClick = {
                    if (isPlaying) {
                        audioPlayer.pause()
                        isPlaying = false
                    } else {
                        audioPlayer.play(file) { isPlaying = false }
                        isPlaying = true
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
