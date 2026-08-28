package com.openlight.notes.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Audio recording (Phase 9).
 * MediaRecorder → m4a, inline playback.
 */
object AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(context: Context): File? {
        val mediaDir = File(context.filesDir, "notes_media").apply { mkdirs() }
        val mediaId = UUID.randomUUID().toString()
        val file = File(mediaDir, "$mediaId.m4a")
        currentFile = file

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        return file
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            currentFile
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            currentFile?.delete()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            recorder?.release()
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
    }

    fun isRecording(): Boolean = recorder != null
}

/**
 * Audio playback.
 */
class AudioPlayer {
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null

    fun play(file: File, onCompletion: () -> Unit = {}) {
        stop()
        onCompletionListener = onCompletion
        mediaPlayer = android.media.MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { onCompletionListener?.invoke() }
            prepare()
            start()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun release() {
        stop()
    }
}
