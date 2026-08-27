package com.openlight.notes.core.ink

import kotlinx.serialization.Serializable

/**
 * Stroke data for one ink block (AD-5, FORMAT.md §3.3).
 * Sovereign format consumed by rendering, ML Kit, and refinement math.
 */
@Serializable
data class StrokeData(
    val strokes: List<Stroke> = emptyList()
) {
    fun addStroke(stroke: Stroke) {
        strokes.add(stroke)
    }

    fun removeStroke(stroke: Stroke) {
        strokes.remove(stroke)
    }
}
