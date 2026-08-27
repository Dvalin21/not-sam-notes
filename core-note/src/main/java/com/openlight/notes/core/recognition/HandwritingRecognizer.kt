package com.openlight.notes.core.recognition

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.digital.InkRecognition
import com.google.mlkit.vision.digital.InkRecognitionModel
import com.google.mlkit.vision.digital.InkRecognitionModelIdentifier
import com.google.mlkit.vision.digital.InkRecognizer
import com.google.mlkit.vision.digital.InkRecognizerOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.openlight.notes.core.ink.Stroke
import kotlinx.coroutines.tasks.await

/**
 * Handwriting recognition using ML Kit Digital Ink Recognition (AD-4).
 * Feature-flagged: app fully functional without it.
 */
class HandwritingRecognizer {
    private var recognizer: InkRecognizer? = null
    private var model: InkRecognitionModel? = null

    suspend fun initialize(context: Context, languageTag: String = "en-US") {
        try {
            val modelIdentifier = InkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                ?: return
            model = InkRecognitionModel.builder(modelIdentifier).build()

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            RemoteModelManager.getInstance().download(model!!, conditions).await()

            recognizer = InkRecognition.getClient(
                InkRecognizerOptions.builder(model!!).build()
            )
        } catch (e: Exception) {
            Log.w("HandwritingRecognizer", "ML Kit init failed: ${e.message}")
        }
    }

    suspend fun recognize(strokes: List<Stroke>): String {
        val recognizer = this.recognizer ?: return ""

        try {
            val ink = com.google.mlkit.vision.digital.Ink.builder()
            strokes.forEach { stroke ->
                val strokeBuilder = com.google.mlkit.vision.digital.Ink.Stroke.builder()
                stroke.points.forEach { point ->
                    strokeBuilder.addPoint(
                        com.google.mlkit.vision.digital.Ink.Point.create(
                            point[0], point[1], point[2].toLong()
                        )
                    )
                }
                ink.addStroke(strokeBuilder.build())
            }

            val result = recognizer.recognize(ink.build()).await()
            return result.candidates.joinToString(" ") { it.text }
        } catch (e: Exception) {
            Log.w("HandwritingRecognizer", "Recognition failed: ${e.message}")
            return ""
        }
    }

    val isAvailable: Boolean get() = recognizer != null
}
