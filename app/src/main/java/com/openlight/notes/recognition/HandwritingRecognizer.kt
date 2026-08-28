package com.openlight.notes.recognition

import android.content.Context
import android.util.Log
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.openlight.notes.core.ink.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handwriting recognition using ML Kit Digital Ink Recognition (AD-4).
 * Feature-flagged: app fully functional without it.
 *
 * Package from jar: com.google.mlkit.vision.digitalink.recognition
 */
class HandwritingRecognizer {
    private var available = false
    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null

    suspend fun initialize(context: Context, languageTag: String = "en-US"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                    ?: return@withContext false
                model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model!!).build()
                )
                available = true
                true
            } catch (e: MlKitException) {
                Log.w("HandwritingRecognizer", "ML Kit not available: ${e.message}")
                available = false
                false
            } catch (e: Exception) {
                Log.w("HandwritingRecognizer", "Init failed: ${e.message}")
                available = false
                false
            }
        }
    }

    suspend fun recognize(strokes: List<Stroke>): String {
        if (!available || recognizer == null || strokes.isEmpty()) return ""
        return withContext(Dispatchers.IO) {
            try {
                val inkBuilder = Ink.builder()
                for (stroke in strokes) {
                    val strokeBuilder = Ink.Stroke.builder()
                    for (point in stroke.points) {
                        strokeBuilder.addPoint(
                            Ink.Point.create(
                                point[0].toFloat(),
                                point[1].toFloat(),
                                if (point.size > 2) point[2].toLong() else 0L
                            )
                        )
                    }
                    inkBuilder.addStroke(strokeBuilder.build())
                }
                val rec = recognizer ?: return@withContext ""
                val result = rec.recognize(inkBuilder.build()).await()
                result.candidates.firstOrNull()?.text ?: ""
            } catch (e: Exception) {
                Log.w("HandwritingRecognizer", "Recognition failed: ${e.message}")
                ""
            }
        }
    }

    val isAvailable: Boolean get() = available
}
