package com.openlight.notes.recognition

import android.content.Context
import com.openlight.notes.core.ink.Stroke

/**
 * Handwriting recognition using ML Kit Digital Ink Recognition (AD-4).
 * Feature-flagged: app fully functional without it.
 *
 * TODO: verify ML Kit API against actual dependency
 */
class HandwritingRecognizer {
    private var available = false

    suspend fun initialize(context: Context, languageTag: String = "en-US") {
        // TODO: implement after device test
    }

    suspend fun recognize(strokes: List<Stroke>): String {
        return ""
    }

    val isAvailable: Boolean get() = available
}
