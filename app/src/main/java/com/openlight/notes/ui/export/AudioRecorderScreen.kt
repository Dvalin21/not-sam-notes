package com.openlight.notes.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openlight.notes.AppContainer
import com.openlight.notes.audio.AudioPlayer
import com.openlight.notes.audio.AudioRecorder
import kotlinx.coroutines.delay
import java.io.File

/**
 * Audio recorder screen (Phase 9): record, play, save audio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(AudioRecorder.isRecording()) }
    var isPlaying by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var playbackPosition by remember { mutableIntStateOf(0) }
    var playbackDuration by remember { mutableIntStateOf(0) }

    val audioPlayer = remember { AudioPlayer() }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordingDuration++
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            playbackPosition = audioPlayer.getCurrentPosition()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Audio") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isRecording) {
                            AudioRecorder.stopRecording()
                        }
                        onBack()
                    }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRecording) "Recording..." else if (recordedFile != null) "Ready to play" else "Tap to record",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isRecording) {
                Text(
                    text = formatDuration(recordingDuration * 1000),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isRecording) {
                    IconButton(onClick = {
                        val file = AudioRecorder.stopRecording()
                        isRecording = false
                        recordedFile = file
                    }) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop Recording"
                        )
                    }
                    Text(
                        text = "Stop",
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                } else {
                    IconButton(onClick = {
                        val file = AudioRecorder.startRecording(context)
                        isRecording = true
                        recordingDuration = 0
                    }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Start Recording"
                        )
                    }
                    Text(
                        text = "Record",
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }
            }

            recordedFile?.let { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Recording",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (isPlaying) {
                            LinearProgressIndicator(
                                progress = {
                                    if (playbackDuration > 0) {
                                        playbackPosition.toFloat() / playbackDuration.toFloat()
                                    } else 0f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    audioPlayer.pause()
                                    isPlaying = false
                                } else {
                                    audioPlayer.play(file) {
                                        isPlaying = false
                                        playbackPosition = 0
                                    }
                                    playbackDuration = audioPlayer.getDuration()
                                    isPlaying = true
                                }
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                )
                            }

                            Text(
                                text = formatDuration(playbackPosition),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            Text(
                                text = " / ${formatDuration(playbackDuration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = {
                                audioPlayer.stop()
                                isPlaying = false
                                AudioRecorder.cancelRecording()
                                recordedFile?.delete()
                                recordedFile = null
                                playbackPosition = 0
                                playbackDuration = 0
                            }) {
                                Text(
                                    text = "Discard",
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }

                            Button(onClick = {
                                // File is already saved; navigate back
                                onBack()
                            }) {
                                Text(
                                    text = "Save",
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
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